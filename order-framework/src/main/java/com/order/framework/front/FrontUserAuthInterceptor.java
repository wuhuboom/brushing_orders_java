package com.order.framework.front;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class FrontUserAuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(FrontUserAuthInterceptor.class);
    private static final String START_TIME = FrontUserAuthInterceptor.class.getName() + ".start";

    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private FrontJwtUtil frontJwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long started = System.nanoTime();
        request.setAttribute(START_TIME, started);
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            return writeUnauthorizedResponse(request, response,
                    "Please access after authorization");
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        FrontJwtUtil.FrontPrincipal principal;
        try {
            principal = frontJwtUtil.authenticate(token);
        } catch (FrontAuthenticationUnavailableException ex) {
            log.error("event=front_auth_unavailable path={} elapsedMs={}",
                    request.getRequestURI(), (System.nanoTime() - started) / 1_000_000, ex);
            response.setHeader("Retry-After", "3");
            return writeJsonResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Authentication temporarily unavailable, please retry");
        }
        if (principal == null) {
            return writeUnauthorizedResponse(request, response,
                    "Invalid or expired token");
        }

        request.setAttribute("userId", principal.userId());
        request.setAttribute("username", principal.username());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception exception) {
        logSlowRequest(request, response);
    }

    private void logSlowRequest(HttpServletRequest request, HttpServletResponse response) {
        Object start = request.getAttribute(START_TIME);
        if (start instanceof Long started) {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            if (elapsedMs >= 1000) {
                log.warn("event=front_api_slow path={} status={} userId={} elapsedMs={}",
                        request.getRequestURI(), response.getStatus(),
                        request.getAttribute("userId"), elapsedMs);
            }
        }
    }

    private boolean writeUnauthorizedResponse(HttpServletRequest request,
            HttpServletResponse response, String message) throws IOException {
        writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, message);
        // Spring does not invoke afterCompletion when this interceptor rejects preHandle.
        logSlowRequest(request, response);
        return false;
    }

    private boolean writeJsonResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format(
                "{\"code\":%d,\"msg\":\"%s\",\"message\":\"%s\"}",
                code, message, message);
        response.getWriter().write(body);
        return false;
    }
}
