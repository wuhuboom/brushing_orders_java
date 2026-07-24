package com.order.api.service;

import com.order.common.i18n.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class ApiLocaleService {
    public SupportedLocale resolve(String explicitLang, HttpServletRequest request) {
        return SupportedLocale.resolve(explicitLang, request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
    }

    public SupportedLocale resolve(HttpServletRequest request) {
        return resolve(request.getParameter("lang"), request);
    }

    public HttpHeaders responseHeaders(SupportedLocale locale) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag());
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        return headers;
    }

    public HttpHeaders legacyHeaders(SupportedLocale locale, String successorPath) {
        HttpHeaders headers = responseHeaders(locale);
        headers.add("Deprecation", "true");
        headers.add(HttpHeaders.LINK, "<" + successorPath + ">; rel=\"successor-version\"");
        return headers;
    }
}
