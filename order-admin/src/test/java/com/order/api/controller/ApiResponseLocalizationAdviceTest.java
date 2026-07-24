package com.order.api.controller;

import com.order.api.service.LocalizedApiMessageService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiResponseLocalizationAdviceTest {

    @Test
    void localizesAnyApiEnvelopeByBusinessCodeAndAddsLanguageHeaders() {
        LocalizedApiMessageService messageService = mock(LocalizedApiMessageService.class);
        when(messageService.message(
                908,
                SupportedLocale.ES_ES,
                "Please try again later (system busy)"))
                .thenReturn("Inténtelo de nuevo más tarde");
        ApiResponseLocalizationAdvice advice = new ApiResponseLocalizationAdvice(messageService);
        AjaxResult body = AjaxResult.error(908, "Please try again later (system busy)");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", "/api/order/createOrder");
        servletRequest.setQueryString("lang=es_ES");
        servletRequest.addHeader("Accept-Language", "fr-FR");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response =
                new ServletServerHttpResponse(new MockHttpServletResponse());

        advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                null,
                request,
                response);

        assertEquals("Inténtelo de nuevo más tarde", body.get("msg"));
        assertEquals("es-ES", response.getHeaders().getFirst("Content-Language"));
        assertEquals("Accept-Language", response.getHeaders().getFirst("Vary"));
    }
}
