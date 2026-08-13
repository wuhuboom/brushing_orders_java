package com.order.api.controller;

import com.order.api.service.OrderApiException;
import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderApiExceptionHandler.class);
    private static final Set<String> CONCURRENCY_CONSTRAINTS = Set.of(
            "uk_order_api_request_key",
            "uk_order_info_pending_user",
            "uk_order_info_order_number",
            "uk_order_bonus_claimable_exact");

    @ExceptionHandler(OrderApiException.class)
    public ResponseEntity<AjaxResult> handle(
            OrderApiException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(AjaxResult.error(
                        exception.getBusinessCode(),
                        PublicApiMessageCatalog.message(exception.getBusinessCode())));
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<AjaxResult> concurrentFailure(
            Exception exception,
            HttpServletRequest request) {
        log.warn("event=order_concurrency_conflict path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
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
            return ApiControllerSupport.error(
                    HttpStatus.CONFLICT, SYSTEM_BUSY, "Please try again later");
        }
        log.error("event=order_integrity_error path={}", request.getRequestURI(), exception);
        return ApiControllerSupport.error(
                HttpStatus.INTERNAL_SERVER_ERROR, 500, "Please try again later");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<AjaxResult> missingRequestHeader(
            MissingRequestHeaderException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AjaxResult.error(
                        INVALID_REQUEST,
                        PublicApiMessageCatalog.message(INVALID_REQUEST)));
    }

}
