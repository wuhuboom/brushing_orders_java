package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigApiException;
import com.order.api.service.LocalizedApiMessageService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@RestControllerAdvice(assignableTypes = ConfigController.class)
public class ConfigApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ConfigApiExceptionHandler.class);
    private static final Set<String> LEGACY_PATHS = Set.of(
            "/getGlobalConfig", "/getConfigByLang", "/getCustomerService",
            "/getCustomerServiceByLang", "/getTradeConfig", "/getLevel",
            "/getNoticeList", "/getNotice/", "/getZoneActive", "/upload");

    private final ApiLocaleService localeService;
    private final LocalizedApiMessageService messageService;

    public ConfigApiExceptionHandler(
            ApiLocaleService localeService,
            LocalizedApiMessageService messageService) {
        this.localeService = localeService;
        this.messageService = messageService;
    }

    @ExceptionHandler(ConfigApiException.class)
    public ResponseEntity<AjaxResult> handle(ConfigApiException exception, HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(request);
        boolean legacy = isLegacy(request);
        HttpStatus status = legacy ? HttpStatus.OK : exception.getHttpStatus();
        String message = messageService.message(
                exception.getCode(), locale, exception.getMessage());
        return ResponseEntity.status(status)
                .headers(responseHeaders(locale, legacy))
                .body(AjaxResult.error(exception.getCode(), message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> validation(
            BindException exception,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(request);
        boolean legacy = isLegacy(request);
        FieldError error = exception.getBindingResult().getFieldError();
        String fallback = error == null ? "Invalid request" : error.getDefaultMessage();
        String message = messageService.message(400, locale, fallback);
        return ResponseEntity.status(legacy ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .headers(responseHeaders(locale, legacy))
                .body(AjaxResult.error(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> unexpected(Exception exception, HttpServletRequest request) {
        log.error("event=config_api_error path={}", request.getRequestURI(), exception);
        SupportedLocale locale = localeService.resolve(request);
        boolean legacy = isLegacy(request);
        HttpStatus status = legacy ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = messageService.message(500, locale, "Please try again later");
        return ResponseEntity.status(status)
                .headers(responseHeaders(locale, legacy))
                .body(AjaxResult.error(500, message));
    }

    private boolean isLegacy(HttpServletRequest request) {
        return ApiControllerSupport.isLegacy(request, "/api/config", LEGACY_PATHS);
    }

    private HttpHeaders responseHeaders(SupportedLocale locale, boolean legacy) {
        HttpHeaders headers = localeService.responseHeaders(locale);
        if (legacy) {
            headers.add("Deprecation", "true");
        }
        return headers;
    }
}
