package com.order.common.utils.html;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;

/** Reads the persisted companion belonging to the same original value and language. */
public final class RichTextRelativeValues {
    private static final ObjectMapper JSON = new ObjectMapper();

    private RichTextRelativeValues() {
    }

    public static String storedOrDerived(String relative, String absolute) {
        return relative != null ? relative : RichTextImagePaths.toRelative(absolute);
    }

    public static String translation(Translations translations, SupportedLocale locale) {
        if (translations == null) {
            return null;
        }
        String stored = translations.getRelativeContent();
        if (stored != null) {
            try {
                JsonNode value = JSON.readTree(stored).path(locale.getCode());
                if (value.isTextual()) {
                    return value.textValue();
                }
            } catch (Exception ignored) {
                // Legacy records without a usable companion still resolve in their original language.
            }
        }
        return RichTextImagePaths.toRelative(locale.read(translations));
    }
}
