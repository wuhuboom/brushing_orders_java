package com.order.member.service.impl;

import com.order.common.config.OrderConfig;
import com.order.common.utils.ServletUtils;
import com.order.common.utils.html.RichTextImagePaths;
import jakarta.servlet.http.HttpServletRequest;

/** Resolves legacy absolute copies only when a deployment supplies a public address. */
final class RichTextStorageSupport {
    private RichTextStorageSupport() {
    }

    static String absolute(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String baseUrl = OrderConfig.getResourceBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            var attributes = ServletUtils.getRequestAttributes();
            if (attributes == null) {
                return value;
            }
            HttpServletRequest request = attributes.getRequest();
            String requestUrl = request.getRequestURL().toString();
            baseUrl = requestUrl.substring(0, requestUrl.length() - request.getRequestURI().length())
                    + request.getContextPath();
        }
        return RichTextImagePaths.toAbsolute(value, baseUrl);
    }
}
