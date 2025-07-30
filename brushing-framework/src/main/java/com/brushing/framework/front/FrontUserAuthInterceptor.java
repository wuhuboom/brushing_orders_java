package com.brushing.framework.front;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class FrontUserAuthInterceptor implements HandlerInterceptor {

   // private static final String TOKEN_PREFIX = "";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please access after authorization");
        }

        String token = authHeader;

        if (!FrontJwtUtil.validateToken(token)) {
            return writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
        }

        String username = FrontJwtUtil.getUsernameFromToken(token);
        request.setAttribute("username", username);

        return true;
    }

    private boolean writeJsonResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", code, message);
        response.getWriter().write(body);
        return false;
    }
}
