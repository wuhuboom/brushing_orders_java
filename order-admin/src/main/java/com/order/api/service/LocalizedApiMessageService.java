package com.order.api.service;

import com.order.common.i18n.SupportedLocale;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;

@Service
public class LocalizedApiMessageService {
    private static final Logger log = LoggerFactory.getLogger(LocalizedApiMessageService.class);
    private final LocalizedMessageCatalogService catalogService;

    public LocalizedApiMessageService(LocalizedMessageCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public String message(int code, SupportedLocale locale, String defaultMessage, Object... arguments) {
        String pattern;
        try {
            pattern = catalogService.catalog(locale).getOrDefault(String.valueOf(code), defaultMessage);
        } catch (RuntimeException exception) {
            log.warn("event=api_message_catalog_unavailable code={} locale={}", code, locale.getCode());
            pattern = defaultMessage;
        }
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        try {
            return new MessageFormat(
                    pattern,
                    Locale.forLanguageTag(locale.toLanguageTag())).format(arguments);
        } catch (IllegalArgumentException exception) {
            log.warn("event=api_message_pattern_invalid code={} locale={}", code, locale.getCode());
            return pattern;
        }
    }
}
