package com.order.api.service;

import org.springframework.http.HttpStatus;

public class OrderApiException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final int businessCode;

    public OrderApiException(HttpStatus httpStatus, int businessCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.businessCode = businessCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getBusinessCode() {
        return businessCode;
    }

    public static OrderApiException badRequest(int code, String message) {
        return new OrderApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static OrderApiException forbidden(int code, String message) {
        return new OrderApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static OrderApiException notFound(int code, String message) {
        return new OrderApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static OrderApiException conflict(int code, String message) {
        return new OrderApiException(HttpStatus.CONFLICT, code, message);
    }

    public static OrderApiException unavailable(int code, String message) {
        return new OrderApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
