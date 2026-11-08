package com.order.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.TranslationResolver;
import com.order.common.i18n.Translations;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.system.domain.SysNotice;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ConfigQueryService {
    private static final Logger log = LoggerFactory.getLogger(ConfigQueryService.class);
    private static final String DEFAULT_CERTIFICATE_TIP = "非正式员工不可以查看证书";
    private static final Map<String, String> CONTENT_FIELDS = Map.ofEntries(
            Map.entry("register", "protocolContent"),
            Map.entry("about", "aboutContent"),
            Map.entry("certificate", "certificateContent"),
            Map.entry("help", "helpContent"),
            Map.entry("terms", "termsContent"),
            Map.entry("event", "eventContent"),
            Map.entry("transaction", "transactionDescription"),
            Map.entry("order", "orderDescription"),
            Map.entry("usage", "usageDescription"));

    private final IOrderConfigService orderConfigService;
    private final IGoodsCustomerServiceService customerServiceService;
    private final IGoodsMemberLevelService levelService;
    private final ISysNoticeService noticeService;
    private final ISysTimeZoneService timeZoneService;
    private final ITranslationsService translationsService;
    private final ObjectMapper objectMapper;

    public ConfigQueryService(
            IOrderConfigService orderConfigService,
            IGoodsCustomerServiceService customerServiceService,
            IGoodsMemberLevelService levelService,
            ISysNoticeService noticeService,
            ISysTimeZoneService timeZoneService,
            ITranslationsService translationsService,
            ObjectMapper objectMapper) {
        this.orderConfigService = orderConfigService;
        this.customerServiceService = customerServiceService;
        this.levelService = levelService;
        this.noticeService = noticeService;
        this.timeZoneService = timeZoneService;
        this.translationsService = translationsService;
        this.objectMapper = objectMapper;
    }

    public List<ConfigApiDtos.LanguageOption> languages() {
        return SupportedLocale.all().stream()
                .map(locale -> new ConfigApiDtos.LanguageOption(
                        locale.getCode(), locale.toLanguageTag(), locale.getLabel(), locale.isH5Enabled()))
                .toList();
    }

    public ConfigApiDtos.GlobalContentResponse content(SupportedLocale locale) {
        List<String> types = new ArrayList<>(CONTENT_FIELDS.keySet());
        types.add("website");
        List<OrderConfig> configs = orderConfigService.selectOrderConfigByTypes(types);
        Map<String, OrderConfig> byType = new LinkedHashMap<>();
        for (OrderConfig config : configs) {
            byType.put(config.getType(), config);
        }
        Map<Long, Translations> translations = translationsFor(
                configs.stream().map(OrderConfig::getTranslationsId).toList());
        OrderConfig certificate = byType.get("certificate");
        String certificateSwitch = certificateSwitch(certificate, byType.get("website"));
        return new ConfigApiDtos.GlobalContentResponse(
                locale.getCode(),
                configField(byType.get("register"), translations, locale, "protocolContent"),
                configField(byType.get("about"), translations, locale, "aboutContent"),
                "1".equals(certificateSwitch) ? ""
                        : configField(certificate, translations, locale, "certificateContent"),
                "1".equals(certificateSwitch) ? certificateTip(certificate, translations, locale) : "",
                configField(byType.get("help"), translations, locale, "helpContent"),
                configField(byType.get("terms"), translations, locale, "termsContent"),
                configField(byType.get("event"), translations, locale, "eventContent"),
                configField(byType.get("transaction"), translations, locale, "transactionDescription"),
                configField(byType.get("order"), translations, locale, "orderDescription"),
                configField(byType.get("usage"), translations, locale, "usageDescription"));
    }

    public ConfigApiDtos.WebsiteConfigResponse website() {
        Map<String, OrderConfig> byType = new LinkedHashMap<>();
        for (OrderConfig config : orderConfigService.selectOrderConfigByTypes(List.of("website", "certificate"))) {
            byType.put(config.getType(), config);
        }
        OrderConfig config = byType.get("website");
        OrderConfig certificate = byType.get("certificate");
        Map<String, Object> values = websiteValues(config);
        return new ConfigApiDtos.WebsiteConfigResponse(
                textValue(firstValue(values, "name", "siteName")),
                textValue(values.get("currencyUnit")),
                textValue(values.get("copyright")),
                textValue(firstValue(values, "logo", "siteLogo")),
                nonNegativeInt(values.get("popUpLimit")),
                textValue(firstValue(values, "popUpImage", "splashAdImage")),
                textValue(firstValue(values, "backgroundImage", "siteBackground")),
                textValue(values.get("h5BackgroundImage")),
                flagValue(values.get("showLogo"), "1"),
                hideImageFlag(values),
                certificateSwitch(certificate, config),
                stringList(firstValue(values, "imageShowTimeRange", "imageDisplayTimeRange")));
    }

    public ConfigApiDtos.CertificateConfigResponse certificate(SupportedLocale locale) {
        Map<String, OrderConfig> byType = new LinkedHashMap<>();
        List<OrderConfig> configs = orderConfigService.selectOrderConfigByTypes(List.of("website", "certificate"));
        for (OrderConfig config : configs) {
            byType.put(config.getType(), config);
        }
        OrderConfig certificate = byType.get("certificate");
        Map<Long, Translations> translations = translationsFor(
                certificate == null || certificate.getTranslationsId() == null
                        ? List.of() : List.of(certificate.getTranslationsId()));
        return new ConfigApiDtos.CertificateConfigResponse(
                certificateSwitch(certificate, byType.get("website")),
                certificateTip(certificate, translations, locale));
    }

    private Map<String, Object> websiteValues(OrderConfig config) {
        return parseMap(
                config == null ? null : config.getContent(),
                config == null ? null : config.getId(),
                "website");
    }

    private String certificateSwitch(OrderConfig certificate, OrderConfig website) {
        Map<String, Object> values = parseMap(
                certificate == null ? null : certificate.getContent(),
                certificate == null ? null : certificate.getId(),
                "certificate");
        // A saved certificate setting, including an explicit disabled value, overrides the old location.
        Object raw = values.containsKey("certificateSwitch")
                ? values.get("certificateSwitch") : websiteValues(website).get("certificateSwitch");
        String value = textValue(raw);
        return "1".equals(value) || "true".equalsIgnoreCase(value) ? "1" : "0";
    }

    private String certificateTip(
            OrderConfig certificate,
            Map<Long, Translations> translations,
            SupportedLocale locale) {
        String tip = configField(certificate, translations, locale, "certificateTip", false);
        return TranslationResolver.hasText(tip) ? tip : DEFAULT_CERTIFICATE_TIP;
    }

    public List<ConfigApiDtos.CustomerServiceResponse> customerServices(SupportedLocale locale) {
        GoodsCustomerService criteria = new GoodsCustomerService();
        criteria.setIsEnabled("1");
        List<GoodsCustomerService> services = customerServiceService.selectGoodsCustomerServiceList(criteria);
        Map<Long, Translations> translations = translationsFor(
                services.stream().map(GoodsCustomerService::getTranslationsId).toList());
        return services.stream().map(service -> {
            Translations record = translations.get(service.getTranslationsId());
            return new ConfigApiDtos.CustomerServiceResponse(
                    service.getId(),
                    localizedEntityField(record, locale, "name", service.getName(), false, service.getId()),
                    service.getSortOrder(),
                    service.getImage(),
                    service.getLink());
        }).toList();
    }

    public List<ConfigApiDtos.MemberLevelResponse> memberLevels(SupportedLocale locale) {
        List<GoodsMemberLevel> levels = levelService.selectGoodsMemberLevelList(null);
        Map<Long, Translations> translations = translationsFor(
                levels.stream().map(GoodsMemberLevel::getTranslationsId).toList());
        return levels.stream()
                .map(level -> memberLevel(level, translations.get(level.getTranslationsId()), locale))
                .toList();
    }

    public ConfigApiDtos.MemberLevelResponse memberLevel(GoodsMemberLevel level, SupportedLocale locale) {
        if (level == null) {
            return null;
        }
        Translations translations = level.getTranslations();
        if (translations == null && level.getTranslationsId() != null) {
            translations = translationsService.selectTranslationsById(level.getTranslationsId());
        }
        return memberLevel(level, translations, locale);
    }

    public void localizeMemberLevelInPlace(GoodsMemberLevel level, SupportedLocale locale) {
        ConfigApiDtos.MemberLevelResponse localized = memberLevel(level, locale);
        if (localized != null) {
            level.setName(localized.name());
            level.setDescription(localized.description());
        }
    }

    public ConfigApiDtos.PagedResult<ConfigApiDtos.NoticeSummaryResponse> notices(
            SupportedLocale locale,
            int pageNum,
            int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        SysNotice criteria = new SysNotice();
        criteria.setStatus("0");
        List<SysNotice> notices = noticeService.selectNoticeList(criteria);
        long total = new PageInfo<>(notices).getTotal();
        Map<Long, Translations> translations = translationsFor(
                notices.stream().map(SysNotice::getTranslationsId).toList());
        List<ConfigApiDtos.NoticeSummaryResponse> rows = notices.stream().map(notice -> {
            String title = localizedEntityField(
                    translations.get(notice.getTranslationsId()),
                    locale,
                    "title",
                    notice.getNoticeTitle(),
                    false,
                    String.valueOf(notice.getNoticeId()));
            return new ConfigApiDtos.NoticeSummaryResponse(notice.getNoticeId(), title, notice.getCreateTime());
        }).toList();
        return new ConfigApiDtos.PagedResult<>(rows, total);
    }

    public ConfigApiDtos.NoticeDetailResponse notice(Long id, SupportedLocale locale) {
        SysNotice notice = noticeService.selectNoticeById(id);
        if (notice == null || !"0".equals(notice.getStatus())) {
            throw new ConfigApiException(701, HttpStatus.NOT_FOUND, "No data");
        }
        Translations translations = notice.getTranslations();
        String reference = String.valueOf(notice.getNoticeId());
        return new ConfigApiDtos.NoticeDetailResponse(
                notice.getNoticeId(),
                localizedEntityField(translations, locale, "title", notice.getNoticeTitle(), false, reference),
                localizedEntityField(translations, locale, "content", notice.getNoticeContent(), false, reference),
                notice.getCreateTime());
    }

    public ConfigApiDtos.TimeZoneResponse timeZone() {
        SysTimeZone active = timeZoneService.getActive();
        if (active == null || !TranslationResolver.hasText(active.getTzName())) {
            throw new ConfigApiException(500, HttpStatus.SERVICE_UNAVAILABLE, "Configuration is unavailable");
        }
        return new ConfigApiDtos.TimeZoneResponse(active.getTzName());
    }

    public ConfigApiDtos.TradeConfigResponse trade() {
        OrderConfig config = orderConfigService.selectOrderConfigByType("trade");
        Map<String, Object> values = parseMap(config == null ? null : config.getContent(),
                config == null ? null : config.getId(), "trade");
        return new ConfigApiDtos.TradeConfigResponse(
                values.get("registerBonusAmount"),
                values.get("minTradeBalance"),
                values.get("memberWithdrawalStatus"),
                values.get("minCreditScoreForWithdrawal"),
                values.get("minWithdrawalAmount"),
                values.get("maxWithdrawalAmount"),
                values.get("withdrawalFeeRate"),
                values.get("serviceTimeRange"),
                values.get("tradeTimeRange"),
                values.get("withdrawalTimeRange"),
                values.get("orderExpireSeconds"),
                values.get("startTaskDelayMs"),
                values.get("submitTaskDelayMs"),
                values.get("requiredTaskGroupsForWithdrawal"),
                values.get("allowModifyWithdrawalAddress"));
    }

    private ConfigApiDtos.MemberLevelResponse memberLevel(
            GoodsMemberLevel level,
            Translations translations,
            SupportedLocale locale) {
        String reference = String.valueOf(level.getId());
        return new ConfigApiDtos.MemberLevelResponse(
                level.getId(),
                localizedEntityField(translations, locale, "name", level.getName(), false, reference),
                level.getLevel(),
                level.getIcon(),
                level.getPrice(),
                level.getMinBalance(),
                level.getInviteCount(),
                level.getOrderCountPerDay(),
                level.getMinCommissionRate(),
                level.getMaxCommissionRate(),
                level.getMinContinuousCommissionRate(),
                level.getMaxContinuousCommissionRate(),
                level.getTaskCountPerDay(),
                level.getWithdrawCountPerDay(),
                level.getWithdrawFeeRate(),
                level.getMinWithdrawAmount(),
                level.getWithdrawLimitPerDay(),
                level.getMinWithdraw(),
                level.getMaxWithdraw(),
                localizedEntityField(translations, locale, "description", level.getDescription(), true, reference),
                level.getProductMatchEnabled(),
                level.getProductMatchMin(),
                level.getProductMatchMax());
    }

    private String configField(
            OrderConfig config,
            Map<Long, Translations> translationsById,
            SupportedLocale locale,
            String field) {
        return configField(config, translationsById, locale, field, true);
    }

    private String configField(
            OrderConfig config,
            Map<Long, Translations> translationsById,
            SupportedLocale locale,
            String field,
            boolean allowRaw) {
        if (config == null) {
            return null;
        }
        Translations translations = config.getTranslationsId() == null
                ? null : translationsById.get(config.getTranslationsId());
        List<String> candidates = TranslationResolver.candidates(translations, locale, config.getContent());
        for (int index = 0; index < candidates.size(); index++) {
            String candidate = candidates.get(index);
            // Legacy raw certificate HTML is valid content, but it must never become a notice.
            if (!allowRaw && (candidate == null || !candidate.stripLeading().startsWith("{"))) {
                continue;
            }
            String candidateLanguage = index == 0
                    ? locale.getCode()
                    : index == candidates.size() - 1 ? "base" : SupportedLocale.EN_US.getCode();
            String value = extractField(
                    candidate,
                    field,
                    allowRaw,
                    "config:" + config.getId() + ":" + config.getType(),
                    candidateLanguage);
            if (TranslationResolver.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String localizedEntityField(
            Translations translations,
            SupportedLocale locale,
            String field,
            String baseValue,
            boolean legacyRawValue,
            String reference) {
        if (translations != null) {
            String requested = extractField(
                    locale.read(translations), field, legacyRawValue, reference, locale.getCode());
            if (TranslationResolver.hasText(requested)) {
                return requested;
            }
            if (locale != SupportedLocale.EN_US) {
                String english = extractField(
                        SupportedLocale.EN_US.read(translations),
                        field,
                        legacyRawValue,
                        reference,
                        SupportedLocale.EN_US.getCode());
                if (TranslationResolver.hasText(english)) {
                    return english;
                }
            }
        }
        return baseValue;
    }

    private String extractField(
            String raw,
            String field,
            boolean allowRaw,
            String reference,
            String language) {
        if (!TranslationResolver.hasText(raw)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root.isObject()) {
                boolean certificateField = "certificateContent".equals(field) || "certificateTip".equals(field);
                JsonNode value = certificateField ? certificateJsonValue(root, field) : root.get(field);
                if (value == null || value.isNull()) {
                    return null;
                }
                return value.isTextual() ? value.asText() : value.toString();
            }
            if (root.isTextual()) {
                return allowRaw ? root.asText() : null;
            }
            return allowRaw ? raw : null;
        } catch (Exception exception) {
            String trimmed = raw.trim();
            if (allowRaw && !trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                return raw;
            }
            log.warn(
                    "event=localized_content_invalid reference={} field={} language={}",
                    reference,
                    field,
                    language);
            return null;
        }
    }

    private JsonNode certificateJsonValue(JsonNode root, String field) {
        List<JsonNode> containers = root.path("value").isObject()
                ? List.of(root, root.get("value")) : List.of(root);
        List<String> fields = "certificateContent".equals(field)
                ? List.of(field, "certificate", "content") : List.of(field);
        for (String candidateField : fields) {
            for (JsonNode container : containers) {
                JsonNode value = container.get(candidateField);
                if (value == null || value.isNull()
                        || (value.isTextual() && !TranslationResolver.hasText(value.asText()))
                        || ("certificateTip".equals(field) && !value.isTextual())) {
                    continue;
                }
                return value;
            }
        }
        return null;
    }

    private Map<Long, Translations> translationsFor(Collection<Long> ids) {
        return translationsService.selectTranslationsByIds(ids);
    }

    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && TranslationResolver.hasText(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String flagValue(Object value, String fallback) {
        String text = textValue(value);
        return text == null ? fallback : text;
    }

    private String hideImageFlag(Map<String, Object> values) {
        String current = textValue(values.get("hideImage"));
        if (current != null) {
            return current;
        }
        String legacy = textValue(values.get("enableImageHide"));
        if (legacy == null) {
            return "0";
        }
        return "0".equals(legacy) ? "1" : "0";
    }

    private int nonNegativeInt(Object value) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(value)));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .map(this::textValue)
                .filter(item -> item != null)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String content, Long configId, String type) {
        if (!TranslationResolver.hasText(content)) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(content, LinkedHashMap.class);
            return values == null ? Map.of() : values;
        } catch (Exception exception) {
            log.warn("event=config_json_invalid configId={} type={}", configId, type);
            return Map.of();
        }
    }
}
