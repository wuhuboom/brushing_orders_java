package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiUploadSizeExceptionHandler {
    private final ApiLocaleService localeService;
    private final LocalizedApiMessageService messageService;

    public ApiUploadSizeExceptionHandler(
            ApiLocaleService localeService,
            LocalizedApiMessageService messageService) {
        this.localeService = localeService;
        this.messageService = messageService;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<AjaxResult> handle(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        SupportedLocale locale = localeService.resolve(request.getParameter("lang"), request);
        boolean publicApi = uri != null && uri.startsWith("/api/");
        AjaxResult result = AjaxResult.error(
                703,
                publicApi
                        ? PublicApiMessageCatalog.message(703)
                        : messageService.message(703, locale, "Upload failed"));
        if (uri != null && uri.endsWith("/api/config/upload")) {
            return ResponseEntity.ok()
                    .headers(localeService.responseHeaders(locale))
                    .body(result);
        }
        if (uri != null && uri.endsWith("/api/user/avatar")) {
            return ResponseEntity.ok()
                    .headers(localeService.responseHeaders(locale))
                    .body(result);
        }
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .headers(localeService.responseHeaders(locale))
                .body(result);
    }
}
