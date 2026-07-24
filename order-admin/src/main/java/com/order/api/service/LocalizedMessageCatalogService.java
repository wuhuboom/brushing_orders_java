package com.order.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.TranslationResolver;
import com.order.common.i18n.Translations;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocalizedMessageCatalogService {
    public static final String CACHE_NAME = "apiMessageCatalog";
    private static final Logger log = LoggerFactory.getLogger(LocalizedMessageCatalogService.class);

    private final IOrderConfigService orderConfigService;
    private final ObjectMapper objectMapper;

    public LocalizedMessageCatalogService(IOrderConfigService orderConfigService, ObjectMapper objectMapper) {
        this.orderConfigService = orderConfigService;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = CACHE_NAME, key = "#locale.code")
    public Map<String, String> catalog(SupportedLocale locale) {
        OrderConfig config = orderConfigService.selectOrderConfigByType("error");
        if (config == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        merge(result, config.getContent(), config.getId(), "base");
        Translations translations = config.getTranslations();
        if (translations != null) {
            merge(result, SupportedLocale.EN_US.read(translations), config.getId(), SupportedLocale.EN_US.getCode());
            if (locale != SupportedLocale.EN_US) {
                merge(result, locale.read(translations), config.getId(), locale.getCode());
            }
        }
        return Map.copyOf(result);
    }

    private void merge(Map<String, String> target, String content, Long configId, String locale) {
        if (!TranslationResolver.hasText(content)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode codes = root.path("codes");
            if (!codes.isArray()) {
                return;
            }
            for (JsonNode item : codes) {
                String code = item.path("code").asText("").trim();
                String message = item.path("message").asText("").trim();
                if (!code.isEmpty() && !message.isEmpty()) {
                    target.put(code, message);
                }
            }
        } catch (Exception exception) {
            log.warn("event=api_message_config_invalid configId={} locale={}", configId, locale);
        }
    }
}
