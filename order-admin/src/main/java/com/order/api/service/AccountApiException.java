package com.order.api.service;

import org.springframework.http.HttpStatus;

public class AccountApiException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final int legacyCode;

    public AccountApiException(HttpStatus httpStatus, int legacyCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.legacyCode = legacyCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getLegacyCode() {
        return legacyCode;
    }

    public static AccountApiException badRequest(int code, String message) {
        return new AccountApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AccountApiException forbidden(int code, String message) {
        return new AccountApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static AccountApiException notFound(int code, String message) {
        return new AccountApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static AccountApiException conflict(int code, String message) {
        return new AccountApiException(HttpStatus.CONFLICT, code, message);
    }
}
