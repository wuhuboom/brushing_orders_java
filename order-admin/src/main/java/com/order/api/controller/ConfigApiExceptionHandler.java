package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigApiException;
import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice(assignableTypes = ConfigController.class)
public class ConfigApiExceptionHandler {
    private final ApiLocaleService localeService;

    public ConfigApiExceptionHandler(ApiLocaleService localeService) {
        this.localeService = localeService;
    }

    @ExceptionHandler(ConfigApiException.class)
    public ResponseEntity<AjaxResult> handle(ConfigApiException exception, HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(request);
        return ResponseEntity.status(exception.getHttpStatus())
                .headers(localeService.responseHeaders(locale))
                .body(AjaxResult.error(
                        exception.getCode(),
                        PublicApiMessageCatalog.message(exception.getCode())));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> validation(
            BindException exception,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(localeService.responseHeaders(locale))
                .body(AjaxResult.error(400, PublicApiMessageCatalog.message(400)));
    }

}
