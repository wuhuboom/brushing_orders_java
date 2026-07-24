package com.order.api.service;

import org.springframework.http.HttpStatus;

public class ConfigApiException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    public ConfigApiException(int code, HttpStatus httpStatus, String defaultMessage) {
        super(defaultMessage);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
