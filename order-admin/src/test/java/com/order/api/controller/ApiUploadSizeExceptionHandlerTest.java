package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.common.i18n.SupportedLocale;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiUploadSizeExceptionHandlerTest {
    @Test
    void oversizedConfigUploadKeepsHttp200AndCode703() {
        LocalizedApiMessageService messages = mock(LocalizedApiMessageService.class);
        ApiUploadSizeExceptionHandler handler =
                new ApiUploadSizeExceptionHandler(new ApiLocaleService(), messages);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/config/upload");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN");

        var response = handler.handle(
                new MaxUploadSizeExceededException(10L * 1024 * 1024), request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(703, response.getBody().get("code"));
        assertEquals("Upload failed", response.getBody().get("msg"));
        assertNull(response.getHeaders().getFirst("Deprecation"));
        assertEquals(SupportedLocale.ZH_CN.toLanguageTag(),
                response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
        verify(messages, never()).message(anyInt(), any(), anyString(), any(Object[].class));
    }

    @Test
    void oversizedManagementUploadKeepsLocalizedMessageAndPayloadTooLargeStatus() {
        LocalizedApiMessageService messages = mock(LocalizedApiMessageService.class);
        when(messages.message(
                703,
                SupportedLocale.ZH_CN,
                "Upload failed"))
                .thenReturn("上传失败");
        ApiUploadSizeExceptionHandler handler =
                new ApiUploadSizeExceptionHandler(new ApiLocaleService(), messages);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/common/upload");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN");

        var response = handler.handle(
                new MaxUploadSizeExceededException(10L * 1024 * 1024), request);

        assertEquals(413, response.getStatusCode().value());
        assertEquals(703, response.getBody().get("code"));
        assertEquals("上传失败", response.getBody().get("msg"));
        assertEquals(SupportedLocale.ZH_CN.toLanguageTag(),
                response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
        verify(messages).message(703, SupportedLocale.ZH_CN, "Upload failed");
    }
}
