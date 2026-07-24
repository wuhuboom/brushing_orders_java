package com.order.api.controller;

import com.order.api.service.OrderApiException;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

import static com.order.api.service.OrderErrorCodes.INVALID_REQUEST;
import static com.order.api.service.OrderErrorCodes.SYSTEM_BUSY;

@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderApiExceptionHandler.class);
    private static final Set<String> LEGACY_PATHS = Set.of(
            "/createOrder", "/submitOrder/", "/getOrderInfos");
    private static final Set<String> CONCURRENCY_CONSTRAINTS = Set.of(
            "uk_order_api_request_key",
            "uk_order_info_pending_user",
            "uk_order_info_order_number",
            "uk_order_bonus_claimable_exact");

    @ExceptionHandler(OrderApiException.class)
    public ResponseEntity<AjaxResult> handle(
            OrderApiException exception,
            HttpServletRequest request) {
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : exception.getHttpStatus();
        return ResponseEntity.status(status)
                .body(AjaxResult.error(
                        exception.getBusinessCode(),
                        localizedMessage(exception, request)));
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<AjaxResult> concurrentFailure(
            Exception exception,
            HttpServletRequest request) {
        log.warn("event=order_concurrency_conflict path={}", request.getRequestURI());
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status)
                .body(AjaxResult.error(SYSTEM_BUSY, "Please try again later"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AjaxResult> integrityFailure(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        boolean concurrencyConflict =
                ApiControllerSupport.hasKnownConstraint(exception, CONCURRENCY_CONSTRAINTS);
        if (concurrencyConflict) {
            log.warn("event=order_concurrency_constraint path={}", request.getRequestURI());
            HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.CONFLICT;
            return ApiControllerSupport.error(status, SYSTEM_BUSY, "Please try again later");
        }
        log.error("event=order_integrity_error path={}", request.getRequestURI(), exception);
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ApiControllerSupport.error(status, 500, "Please try again later");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<AjaxResult> missingRequestHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request) {
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(AjaxResult.error(INVALID_REQUEST, "Missing required request header"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> unexpected(
            Exception exception,
            HttpServletRequest request) {
        log.error("event=order_api_error path={}", request.getRequestURI(), exception);
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(AjaxResult.error(500, "Please try again later"));
    }

    private boolean isLegacy(HttpServletRequest request) {
        return ApiControllerSupport.isLegacy(request, "/api/order", LEGACY_PATHS);
    }

    private String localizedMessage(
            OrderApiException exception,
            HttpServletRequest request) {
        SupportedLocale locale = SupportedLocale.resolve(
                request.getParameter("lang"),
                request.getHeader("Accept-Language"));
        int code = exception.getBusinessCode();
        if (locale == SupportedLocale.ZH_CN) {
            return switch (code) {
                case 921 -> "\u5f69\u91d1\u4e0d\u5b58\u5728\u6216\u4e0d\u5c5e\u4e8e\u5f53\u524d\u7528\u6237";
                case 922 -> "\u5f69\u91d1\u5df2\u8fc7\u671f\u6216\u4e0d\u53ef\u9886\u53d6";
                case 923 -> "\u8bf7\u6c42\u53c2\u6570\u65e0\u6548";
                default -> exception.getMessage();
            };
        }
        if (locale == SupportedLocale.ZH_TW) {
            return switch (code) {
                case 921 -> "\u5f69\u91d1\u4e0d\u5b58\u5728\u6216\u4e0d\u5c6c\u65bc\u7576\u524d\u7528\u6236";
                case 922 -> "\u5f69\u91d1\u5df2\u904e\u671f\u6216\u4e0d\u53ef\u9818\u53d6";
                case 923 -> "\u8acb\u6c42\u53c3\u6578\u7121\u6548";
                default -> exception.getMessage();
            };
        }
        return exception.getMessage();
    }
}
