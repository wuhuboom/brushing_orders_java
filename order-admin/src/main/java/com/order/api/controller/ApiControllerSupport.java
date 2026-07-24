package com.order.api.controller;

import com.order.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Set;

final class ApiControllerSupport {
    private ApiControllerSupport() {
    }

    static boolean isLegacy(
            HttpServletRequest request,
            String prefix,
            Set<String> legacyPaths) {
        String path = request.getRequestURI();
        int prefixIndex = path.indexOf(prefix);
        String relativePath = prefixIndex >= 0
                ? path.substring(prefixIndex + prefix.length())
                : path;
        return legacyPaths.stream().anyMatch(marker -> relativePath.equals(marker)
                || marker.endsWith("/") && relativePath.startsWith(marker));
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
