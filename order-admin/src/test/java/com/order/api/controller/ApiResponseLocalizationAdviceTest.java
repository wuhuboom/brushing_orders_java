package com.order.api.controller;

import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import com.order.web.controller.advice.PhoneMaskingResponseBodyAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiResponseLocalizationAdviceTest {

    @Test
    void canonicalizesAnyApiEnvelopeToEnglishAndAddsLanguageHeaders() {
        ApiResponseLocalizationAdvice advice = new ApiResponseLocalizationAdvice();
        AjaxResult body = AjaxResult.error(908, "系统繁忙，请稍后重试");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "POST", "/api/order");
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

        assertEquals("Please try again later", body.get("msg"));
        assertEquals("es-ES", response.getHeaders().getFirst("Content-Language"));
        assertEquals("Accept-Language", response.getHeaders().getFirst("Vary"));
    }

    @Test
    void canonicalizesPageMessagesAndLeavesManagementResponsesUntouched() {
        ApiResponseLocalizationAdvice advice = new ApiResponseLocalizationAdvice();
        com.order.common.core.page.TableDataInfo page = new com.order.common.core.page.TableDataInfo();
        page.setCode(200);
        page.setMsg("查询成功");
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/order");

        advice.beforeBodyWrite(
                page, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(apiRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertEquals("Success", page.getMsg());

        AjaxResult management = AjaxResult.success("操作成功");
        MockHttpServletRequest managementRequest = new MockHttpServletRequest(
                "GET", "/system/user/list");
        ServletServerHttpResponse managementResponse =
                new ServletServerHttpResponse(new MockHttpServletResponse());
        Object unchanged = advice.beforeBodyWrite(
                management, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(managementRequest),
                managementResponse);
        assertSame(management, unchanged);
        assertEquals("操作成功", management.get("msg"));
        assertNull(managementResponse.getHeaders().getFirst("Content-Language"));
        assertNull(managementResponse.getHeaders().getFirst("Vary"));
    }

    @Test
    void runsBeforeAdviceThatCanConvertEnvelopesToJsonNodes() {
        ApiResponseLocalizationAdvice apiAdvice = new ApiResponseLocalizationAdvice();
        PhoneMaskingResponseBodyAdvice jsonNodeAdvice = new PhoneMaskingResponseBodyAdvice();
        List<Object> adviceChain = new ArrayList<>(List.of(jsonNodeAdvice, apiAdvice));

        AnnotationAwareOrderComparator.sort(adviceChain);

        assertSame(apiAdvice, adviceChain.get(0));
        assertSame(jsonNodeAdvice, adviceChain.get(1));
    }
}
