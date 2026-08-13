package com.order.api.controller;

import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.i18n.SupportedLocale;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces stable English messages on every public H5 API response envelope.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiResponseLocalizationAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!request.getURI().getPath().startsWith("/api/")) {
            return body;
        }
        SupportedLocale locale = resolveLocale(request);
        applyLanguageHeaders(response.getHeaders(), locale);

        if (body instanceof AjaxResult result) {
            Object codeValue = result.get(AjaxResult.CODE_TAG);
            if (codeValue instanceof Number code) {
                result.put(
                        AjaxResult.MSG_TAG,
                        PublicApiMessageCatalog.message(code.intValue()));
            }
        } else if (body instanceof TableDataInfo table) {
            table.setMsg(PublicApiMessageCatalog.message(Math.toIntExact(table.getCode())));
        }
        return body;
    }

    private SupportedLocale resolveLocale(ServerHttpRequest request) {
        String lang = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("lang");
        return SupportedLocale.resolve(
                lang,
                request.getHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE));
    }

    private void applyLanguageHeaders(HttpHeaders headers, SupportedLocale locale) {
        headers.set(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag());
        List<String> vary = new ArrayList<>(headers.getVary());
        if (vary.stream().noneMatch(HttpHeaders.ACCEPT_LANGUAGE::equalsIgnoreCase)) {
            vary.add(HttpHeaders.ACCEPT_LANGUAGE);
            headers.setVary(vary);
        }
    }
}
