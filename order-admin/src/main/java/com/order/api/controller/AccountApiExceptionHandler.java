package com.order.api.controller;

import com.order.api.service.AccountApiException;
import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.order.web.controller.member.OrderWithdrawalController;

import java.util.Set;

import static com.order.api.service.AccountErrorCodes.INVALID_REQUEST;
import static com.order.api.service.AccountErrorCodes.IDEMPOTENCY_CONFLICT;

@RestControllerAdvice(assignableTypes = {AccountController.class, OrderWithdrawalController.class})
public class AccountApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AccountApiExceptionHandler.class);
    private static final Set<String> LEGACY_PATHS = Set.of(
            "/withdrawalType", "/addWalletBank", "/getUserBankWallet",
            "/getBankWallet/", "/delBankWallet/", "/withdrawal",
            "/getWithdrawals", "/getDeposit", "/getTransactions");
    private static final Set<String> CONFLICT_CONSTRAINTS =
            Set.of("uk_withdrawal_user_request");

    @ExceptionHandler(AccountApiException.class)
    public ResponseEntity<AjaxResult> handle(AccountApiException exception, HttpServletRequest request) {
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : exception.getHttpStatus();
        return ApiControllerSupport.error(
                status, exception.getLegacyCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AjaxResult> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "Invalid request" : fieldError.getDefaultMessage();
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ApiControllerSupport.error(status, INVALID_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> bindingValidation(
            BindException exception,
            HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "Invalid request" : fieldError.getDefaultMessage();
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ApiControllerSupport.error(status, INVALID_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AjaxResult> unreadable(HttpServletRequest request) {
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ApiControllerSupport.error(status, INVALID_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AjaxResult> integrityFailure(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        if (ApiControllerSupport.hasKnownConstraint(exception, CONFLICT_CONSTRAINTS)) {
            log.warn("event=account_concurrency_constraint path={}", request.getRequestURI());
            HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.CONFLICT;
            return ApiControllerSupport.error(
                    status, IDEMPOTENCY_CONFLICT, "Request was already submitted");
        }
        log.error("event=account_integrity_error path={}", request.getRequestURI(), exception);
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ApiControllerSupport.error(status, 500, "Please try again later");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> unexpected(Exception exception, HttpServletRequest request) {
        log.error("event=account_api_error path={}", request.getRequestURI(), exception);
        HttpStatus status = isLegacy(request) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ApiControllerSupport.error(status, 500, "Please try again later");
    }

    private boolean isLegacy(HttpServletRequest request) {
        return ApiControllerSupport.isLegacy(request, "/api/account", LEGACY_PATHS);
    }
}
