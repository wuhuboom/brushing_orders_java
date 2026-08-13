package com.order.api.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicApiMessageCatalogTest {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    @Test
    void everyCanonicalAndFallbackMessageIsNonChinese() throws Exception {
        Field field = PublicApiMessageCatalog.class.getDeclaredField("MESSAGES");
        field.setAccessible(true);
        Map<?, ?> messages = (Map<?, ?>) field.get(null);
        for (Map.Entry<?, ?> entry : messages.entrySet()) {
            assertNonChinese("code " + entry.getKey(), String.valueOf(entry.getValue()));
        }
        for (int code : new int[] {0, 202, 400, 401, 403, 404, 405, 409, 422, 500, 599, 777}) {
            assertNonChinese("fallback code " + code, PublicApiMessageCatalog.message(code));
        }
    }

    private static void assertNonChinese(String source, String message) {
        assertFalse(HAN.matcher(message).find(), source + " returned Chinese: " + message);
    }
}
