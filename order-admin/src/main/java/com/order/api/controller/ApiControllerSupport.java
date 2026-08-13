package com.order.api.controller;

import com.order.common.core.domain.AjaxResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Set;

final class ApiControllerSupport {
    private ApiControllerSupport() {
    }

    static boolean hasKnownConstraint(Throwable throwable, Set<String> constraints) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (constraints.stream().anyMatch(normalized::contains)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    static ResponseEntity<AjaxResult> error(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(AjaxResult.error(code, message));
    }
}
