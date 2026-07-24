package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.common.i18n.SupportedLocale;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiUploadSizeExceptionHandlerTest {
    @Test
    void oversizedLegacyConfigUploadKeepsHttp200AndCode703() {
        LocalizedApiMessageService messages = mock(LocalizedApiMessageService.class);
        when(messages.message(anyInt(), any(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        ApiUploadSizeExceptionHandler handler =
                new ApiUploadSizeExceptionHandler(new ApiLocaleService(), messages);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/config/upload");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN");

        var response = handler.handle(
                new MaxUploadSizeExceededException(10L * 1024 * 1024), request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(703, response.getBody().get("code"));
        assertEquals("true", response.getHeaders().getFirst("Deprecation"));
        assertEquals(SupportedLocale.ZH_CN.toLanguageTag(),
                response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
    }
}
