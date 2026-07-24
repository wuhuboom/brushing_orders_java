package com.order.common.i18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves localized values using requested locale, English and base content.
 */
public final class TranslationResolver {
    private TranslationResolver() {
    }

    public static String resolve(Translations translations, SupportedLocale locale, String baseValue) {
        for (String candidate : candidates(translations, locale, baseValue)) {
            if (hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static List<String> candidates(
            Translations translations,
            SupportedLocale locale,
            String baseValue) {
        List<String> values = new ArrayList<>(3);
        if (translations != null) {
            values.add(locale.read(translations));
            if (locale != SupportedLocale.EN_US) {
                values.add(SupportedLocale.EN_US.read(translations));
            }
        }
        values.add(baseValue);
        return values;
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
