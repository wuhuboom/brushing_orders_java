package com.order.api.controller;

import com.order.common.core.domain.AjaxResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicApiFallbackExceptionHandlerTest {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");
    private final PublicApiFallbackExceptionHandler handler =
            new PublicApiFallbackExceptionHandler();

    @Test
    void unexpectedExceptionDoesNotExposeChineseExceptionText() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/goods/getGoodsList");

        ResponseEntity<AjaxResult> response =
                handler.unexpected(new RuntimeException("数据库连接异常"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals(500, response.getBody().get("code"));
        assertEquals("Please try again later", response.getBody().get("msg"));
        assertFalse(HAN.matcher(String.valueOf(response.getBody().get("msg"))).find());
    }

    @Test
    void frameworkValidationUsesCanonicalEnglishMessage() {
        ResponseEntity<AjaxResult> response =
                handler.validation(new IllegalArgumentException("参数不能为空"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid request", response.getBody().get("msg"));
    }
}
