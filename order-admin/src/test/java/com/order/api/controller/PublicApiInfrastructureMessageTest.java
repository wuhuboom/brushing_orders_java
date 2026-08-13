package com.order.api.controller;

import com.order.common.annotation.RepeatSubmit;
import com.order.framework.interceptor.RepeatSubmitInterceptor;
import com.order.framework.security.handle.AuthenticationEntryPointImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.HandlerMethod;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PublicApiInfrastructureMessageTest {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    @Test
    void securityEntryPointUsesEnglishForPublicApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/order");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthenticationEntryPointImpl().commence(
                request, response, mock(AuthenticationException.class));

        assertEnglish(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }

    @Test
    void repeatSubmitInterceptorUsesEnglishForPublicApi() throws Exception {
        RepeatSubmitInterceptor interceptor = new RepeatSubmitInterceptor() {
            @Override
            public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation) {
                return true;
            }
        };
        HandlerMethod handler = new HandlerMethod(
                new RepeatFixture(), RepeatFixture.class.getMethod("submit"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEnglish(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Request was already submitted"));
    }

    private static void assertEnglish(String body) {
        assertFalse(HAN.matcher(body).find(), "Public API response contains Chinese: " + body);
    }

    static final class RepeatFixture {
        @RepeatSubmit
        public void submit() {
        }
    }
}
