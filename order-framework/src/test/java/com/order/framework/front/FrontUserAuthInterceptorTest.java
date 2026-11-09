package com.order.framework.front;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrontUserAuthInterceptorTest {
    @Test
    void dependencyFailureReturns503WithoutInvalidatingClientSession() throws Exception {
        FrontJwtUtil jwt = mock(FrontJwtUtil.class);
        FrontUserAuthInterceptor interceptor = new FrontUserAuthInterceptor();
        var field = FrontUserAuthInterceptor.class.getDeclaredField("frontJwtUtil");
        field.setAccessible(true);
        field.set(interceptor, jwt);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(request.getHeader("Authorization")).thenReturn("Bearer probe");
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(jwt.authenticate("probe")).thenThrow(
                new FrontAuthenticationUnavailableException(new IllegalStateException("test dependency failure")));
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(503);
        verify(response).setHeader("Retry-After", "3");
        assertTrue(body.toString().contains("\"code\":503"));
        assertFalse(body.toString().contains("test dependency failure"));
        verify(jwt, never()).invalidateAllTokens(any(), any());
    }
}
