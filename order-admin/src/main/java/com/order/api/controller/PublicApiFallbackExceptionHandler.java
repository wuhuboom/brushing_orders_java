package com.order.api.controller;

import com.order.api.service.ConfigApiException;
import com.order.api.service.PublicApiMessageCatalog;
import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Safe final exception boundary for the six public H5 controllers.
 *
 * <p>Controller-specific handlers keep their business status codes. This
 * handler covers framework and unexpected exceptions without reflecting
 * localized validation, exception, or database text to {@code msg}.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RestControllerAdvice(assignableTypes = {
        AccountController.class,
        AuthController.class,
        ConfigController.class,
        GoodsApiController.class,
        OrderController.class,
        SiteMessageController.class
})
public class PublicApiFallbackExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(PublicApiFallbackExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<AjaxResult> validation(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, 400);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingPathVariableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<AjaxResult> malformedRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, 400);
    }

    @ExceptionHandler(ConfigApiException.class)
    public ResponseEntity<AjaxResult> configFailure(ConfigApiException exception) {
        return error(exception.getHttpStatus(), exception.getCode());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<AjaxResult> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, 405);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> unexpected(
            Exception exception,
            HttpServletRequest request) {
        log.error("event=public_api_unexpected path={}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, 500);
    }

    private ResponseEntity<AjaxResult> error(HttpStatus status, int code) {
        return ResponseEntity.status(status)
                .body(AjaxResult.error(code, PublicApiMessageCatalog.message(code)));
    }
}
