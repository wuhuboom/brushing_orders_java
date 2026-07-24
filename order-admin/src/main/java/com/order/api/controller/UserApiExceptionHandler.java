package com.order.api.controller;

import com.order.api.service.UserApiException;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import com.order.common.i18n.SupportedLocale;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthController.class)
public class UserApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(UserApiExceptionHandler.class);
    private final ApiLocaleService localeService;
    private final LocalizedApiMessageService messageService;

    public UserApiExceptionHandler(
            ApiLocaleService localeService,
            LocalizedApiMessageService messageService) {
        this.localeService = localeService;
        this.messageService = messageService;
    }

    @ExceptionHandler(UserApiException.class)
    public ResponseEntity<AjaxResult> handleUserApiException(
            UserApiException exception,
            HttpServletRequest request) {
        return error(
                exception.getCode(),
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AjaxResult> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        FieldError error = exception.getBindingResult().getFieldError();
        String field = error == null ? "" : error.getField();
        String message = error == null ? "Invalid request" : error.getDefaultMessage();
        return error(validationCode(request.getRequestURI(), field, message), message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AjaxResult> handleUnreadableBody(HttpServletRequest request) {
        return error(617, "Invalid request body", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        log.error("event=front_user_api_error path={}", request.getRequestURI(), exception);
        return error(617, "Unknown error", request);
    }

    private int validationCode(String path, String field, String message) {
        if (path.endsWith("/login")) {
            return 601;
        }
        if (path.endsWith("/register")) {
            return switch (field) {
                case "username" -> 603;
                case "password" -> 604;
                case "tradePassword" -> 605;
                case "phoneNumber" -> 606;
                case "gender" -> 607;
                case "inviteCode" -> 608;
                default -> 617;
            };
        }
        if (path.endsWith("/updateAvatar")) {
            return 614;
        }
        if (field.toLowerCase().contains("password")) {
            return message != null && message.toLowerCase().contains("length") ? 612 : 611;
        }
        return 617;
    }

    private ResponseEntity<AjaxResult> error(
            int code,
            String defaultMessage,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(request);
        String message = messageService.message(code, locale, defaultMessage);
        return ResponseEntity.ok()
                .headers(localeService.responseHeaders(locale))
                .body(AjaxResult.error(code, message));
    }
}
