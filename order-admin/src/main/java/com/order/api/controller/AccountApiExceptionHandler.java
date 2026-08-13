package com.order.api.controller;

import com.order.api.service.AccountApiException;
import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

import static com.order.api.service.AccountErrorCodes.INVALID_REQUEST;
import static com.order.api.service.AccountErrorCodes.IDEMPOTENCY_CONFLICT;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice(assignableTypes = AccountController.class)
public class AccountApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AccountApiExceptionHandler.class);
    private static final Set<String> CONFLICT_CONSTRAINTS =
            Set.of("uk_withdrawal_user_request");

    @ExceptionHandler(AccountApiException.class)
    public ResponseEntity<AjaxResult> handle(AccountApiException exception, HttpServletRequest request) {
        return ApiControllerSupport.error(
                exception.getHttpStatus(), exception.getLegacyCode(),
                responseMessage(request, exception.getLegacyCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<AjaxResult> validation(Exception exception) {
        return ApiControllerSupport.error(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                PublicApiMessageCatalog.message(INVALID_REQUEST));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AjaxResult> unreadable() {
        return ApiControllerSupport.error(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                PublicApiMessageCatalog.message(INVALID_REQUEST));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AjaxResult> integrityFailure(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        if (ApiControllerSupport.hasKnownConstraint(exception, CONFLICT_CONSTRAINTS)) {
            log.warn("event=account_concurrency_constraint path={}", request.getRequestURI());
            return ApiControllerSupport.error(
                    HttpStatus.CONFLICT, IDEMPOTENCY_CONFLICT, "Request was already submitted");
        }
        log.error("event=account_integrity_error path={}", request.getRequestURI(), exception);
        return ApiControllerSupport.error(
                HttpStatus.INTERNAL_SERVER_ERROR, 500, "Please try again later");
    }

    private String responseMessage(HttpServletRequest request, int code, String fallback) {
        return request.getRequestURI().startsWith("/api/")
                ? PublicApiMessageCatalog.message(code)
                : fallback;
    }
}
