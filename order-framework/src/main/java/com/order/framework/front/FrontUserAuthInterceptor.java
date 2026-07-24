package com.order.framework.front;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class FrontUserAuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private FrontJwtUtil frontJwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            return writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please access after authorization");
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        FrontJwtUtil.FrontPrincipal principal = frontJwtUtil.authenticate(token);
        if (principal == null) {
            return writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
        }

        request.setAttribute("userId", principal.userId());
        request.setAttribute("username", principal.username());

        return true;
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
