package com.order.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.order.common.i18n.Translations;
import com.order.member.domain.OrderConfig;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates member site messages from the notification templates in website settings.
 */
@Service
public class SiteMessageNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SiteMessageNotificationService.class);
    private static final Map<String, String> TRANSACTION_TEMPLATE_KEYS = templateKeys();

    private final IOrderConfigService configService;
    private final IOrderSiteMessageService messageService;
    private final OrderUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public SiteMessageNotificationService(
            IOrderConfigService configService,
            IOrderSiteMessageService messageService,
            OrderUserMapper userMapper,
            ObjectMapper objectMapper) {
        this.configService = configService;
        this.messageService = messageService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public void createForTransaction(
            Long userId,
            String transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter) {
        try {
            String templateKey = resolveTemplateKey(transactionType, amount);
            OrderConfig config = configService.selectOrderConfigByType("notification");
            if (config == null || !hasText(config.getContent())) {
                return;
            }
            JsonNode template = objectMapper.readTree(config.getContent()).path(templateKey);
            if (!isEnabled(template)) {
                return;
            }
            OrderUser user = userMapper.selectOrderUserById(userId);
            if (user == null) {
                return;
            }
            Map<String, String> tokens = tokens(
                    user, amount, balanceBefore, balanceAfter,
                    template.path("formatAmount").asInt(0) == 1);
            String title = render(template.path("title").asText(""), tokens);
            String content = render(template.path("content").asText(""), tokens);
            if (!hasText(title) || !hasText(content)) {
                return;
            }

            OrderSiteMessage message = new OrderSiteMessage();
            message.setTitle(title);
            message.setContent(content);
            message.setMemberList(String.valueOf(userId));
            message.setIsEnabled(1);
            message.setTranslations(renderTranslations(
                    config.getTranslations(), templateKey, tokens));
            messageService.insertOrderSiteMessage(message);
        } catch (Exception exception) {
            log.warn("event=site_message_notification_skipped userId={} transactionType={} reason={}",
                    userId, transactionType, exception.getMessage());
        }
    }

    String resolveTemplateKey(String transactionType, BigDecimal amount) {
        String normalized = transactionType == null
                ? "" : transactionType.trim().toLowerCase(Locale.ROOT);
        String mapped = TRANSACTION_TEMPLATE_KEYS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        return amount != null && amount.signum() < 0 ? "deduction" : "other";
    }

    private boolean isEnabled(JsonNode template) {
        return template.isObject()
                && template.path("enabled").asInt(0) == 1
                && hasText(template.path("title").asText(""))
                && hasText(template.path("content").asText(""));
    }

    private Map<String, String> tokens(
            OrderUser user,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            boolean formatAmount) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("username", value(user.getUsername()));
        values.put("phone", value(user.getPhoneNumber()));
        values.put("amount", amount(amount, formatAmount));
        values.put("beforeBalance", amount(before, formatAmount));
        values.put("afterBalance", amount(after, formatAmount));
        values.put("currencyUnit", currencyUnit());
        return values;
    }

    private String currencyUnit() {
        try {
            OrderConfig website = configService.selectOrderConfigByType("website");
            if (website != null && hasText(website.getContent())) {
                return objectMapper.readTree(website.getContent())
                        .path("currencyUnit").asText("");
            }
        } catch (Exception ignored) {
            // Currency is optional in notification templates.
        }
        return "";
    }

    private Translations renderTranslations(
            Translations source,
            String templateKey,
            Map<String, String> tokens) {
        if (source == null) {
            return null;
        }
        Translations result = new Translations();
        renderTranslation(source.getZhCn(), result::setZhCn, templateKey, tokens);
        renderTranslation(source.getZhTw(), result::setZhTw, templateKey, tokens);
        renderTranslation(source.getKoKr(), result::setKoKr, templateKey, tokens);
        renderTranslation(source.getThTh(), result::setThTh, templateKey, tokens);
        renderTranslation(source.getJaJp(), result::setJaJp, templateKey, tokens);
        renderTranslation(source.getPtPt(), result::setPtPt, templateKey, tokens);
        renderTranslation(source.getEnUs(), result::setEnUs, templateKey, tokens);
        renderTranslation(source.getArSa(), result::setArSa, templateKey, tokens);
        renderTranslation(source.getEsEs(), result::setEsEs, templateKey, tokens);
        renderTranslation(source.getSvSe(), result::setSvSe, templateKey, tokens);
        renderTranslation(source.getItIt(), result::setItIt, templateKey, tokens);
        renderTranslation(source.getDeDe(), result::setDeDe, templateKey, tokens);
        renderTranslation(source.getNoNo(), result::setNoNo, templateKey, tokens);
        renderTranslation(source.getRuRu(), result::setRuRu, templateKey, tokens);
        renderTranslation(source.getHuHu(), result::setHuHu, templateKey, tokens);
        renderTranslation(source.getPlPl(), result::setPlPl, templateKey, tokens);
        renderTranslation(source.getSkSk(), result::setSkSk, templateKey, tokens);
        renderTranslation(source.getFrFr(), result::setFrFr, templateKey, tokens);
        renderTranslation(source.getCsCz(), result::setCsCz, templateKey, tokens);
        renderTranslation(source.getPtBr(), result::setPtBr, templateKey, tokens);
        renderTranslation(source.getHiIn(), result::setHiIn, templateKey, tokens);
        return result.hasAnyValue() ? result : null;
    }

    private void renderTranslation(
            String raw,
            Consumer<String> setter,
            String templateKey,
            Map<String, String> tokens) {
        if (!hasText(raw)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode template = root.has("notices") ? root.path("notices").path(templateKey)
                    : root.path(templateKey);
            if (!template.isObject()) {
                return;
            }
            String title = render(template.path("title").asText(""), tokens);
            String content = render(template.path("content").asText(""), tokens);
            if (!hasText(title) && !hasText(content)) {
                return;
            }
            ObjectNode translated = objectMapper.createObjectNode();
            translated.put("title", title);
            translated.put("content", content);
            setter.accept(objectMapper.writeValueAsString(translated));
        } catch (Exception exception) {
            log.debug("event=site_message_translation_skipped templateKey={}", templateKey);
        }
    }

    private String render(String template, Map<String, String> tokens) {
        String result = template == null ? "" : template;
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            result = result.replace("${" + token.getKey() + "}", token.getValue());
            result = result.replace("{" + token.getKey() + "}", token.getValue());
        }
        return result;
    }

    private String amount(BigDecimal value, boolean formatted) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        if (formatted) {
            return new DecimalFormat("#,##0.00").format(safe);
        }
        return safe.toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Map<String, String> templateKeys() {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("zs", "gift");
        keys.put("kk", "deduction");
        keys.put("kc", "deduction");
        keys.put("cz", "recharge");
        keys.put("txz", "withdrawing");
        keys.put("txjd", "withdrawalUnfreeze");
        keys.put("tx", "withdrawal");
        keys.put("txbh", "withdrawalUnfreeze");
        keys.put("txwc", "withdrawal");
        keys.put("rw", "task");
        keys.put("bjfh", "principalReturn");
        keys.put("fy", "rebate");
        keys.put("xjfy", "subRebate");
        keys.put("qd", "signIn");
        keys.put("sxf", "fee");
        keys.put("ck", "deposit");
        keys.put("jj", "bonus");
        keys.put("bonus", "bonus");
        keys.put("dx", "baseSalary");
        keys.put("yzj", "aid");
        keys.put("zczs", "registerBonus");
        keys.put("spfr", "productShare");
        keys.put("rwjl", "taskReward");
        keys.put("yebzc", "balanceOut");
        keys.put("yebzr", "balanceIn");
        keys.put("gzjl", "workBonus");
        keys.put("gzjj", "workBonus");
        keys.put("sjjl", "upgradeBonus");
        keys.put("sjjj", "upgradeBonus");
        keys.put("bt", "subsidy");
        keys.put("zjyc", "abnormalDeposit");
        keys.put("hd", "activity");
        keys.put("jfdh", "pointsExchange");
        keys.put("dlqd", "loginSignIn");
        keys.put("rwwcqd", "taskSignIn");
        keys.put("xyd", "creditScore");
        return Map.copyOf(keys);
    }
}
