package com.order.api.controller;

import com.order.api.service.LocalizedApiMessageService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.i18n.SupportedLocale;
import org.springframework.core.MethodParameter;
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
 * Applies the configured API message catalog to every front-end API envelope.
 */
@RestControllerAdvice(basePackages = "com.order.api.controller")
public class ApiResponseLocalizationAdvice implements ResponseBodyAdvice<Object> {
    private final LocalizedApiMessageService messageService;

    public ApiResponseLocalizationAdvice(LocalizedApiMessageService messageService) {
        this.messageService = messageService;
    }

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
        SupportedLocale locale = resolveLocale(request);
        applyLanguageHeaders(response.getHeaders(), locale);

        if (body instanceof AjaxResult result) {
            Object codeValue = result.get(AjaxResult.CODE_TAG);
            if (codeValue instanceof Number code) {
                Object currentMessage = result.get(AjaxResult.MSG_TAG);
                result.put(
                        AjaxResult.MSG_TAG,
                        messageService.message(
                                code.intValue(),
                                locale,
                                currentMessage instanceof String message ? message : defaultMessage(code.intValue())));
            }
        } else if (body instanceof TableDataInfo table) {
            table.setMsg(messageService.message(
                    Math.toIntExact(table.getCode()),
                    locale,
                    table.getMsg() == null ? defaultMessage(Math.toIntExact(table.getCode())) : table.getMsg()));
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

    private String defaultMessage(int code) {
        return code == 200 ? "Success" : "Please try again later";
    }
}
