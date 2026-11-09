package com.order.framework.front;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrontUserAuthInterceptorTest {
    private static final String START_TIME = FrontUserAuthInterceptor.class.getName() + ".start";

    private final FrontJwtUtil jwt = mock(FrontJwtUtil.class);
    private final FrontUserAuthInterceptor interceptor = new FrontUserAuthInterceptor();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final StringWriter body = new StringWriter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(FrontUserAuthInterceptor.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void setUp() throws Exception {
        var field = FrontUserAuthInterceptor.class.getDeclaredField("frontJwtUtil");
        field.setAccessible(true);
        field.set(interceptor, jwt);
        when(request.getHeader("Authorization")).thenReturn("Bearer probe");
        when(request.getRequestURI()).thenReturn("/api/order");
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
    }

    @Test
    void dependencyFailureReturns503WithoutInvalidatingClientSession() throws Exception {
        simulateSlowRequest();
        when(jwt.authenticate("probe")).thenThrow(
                new FrontAuthenticationUnavailableException(new IllegalStateException("test dependency failure")));
        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(503);
        verify(response).setHeader("Retry-After", "3");
        assertTrue(body.toString().contains("\"code\":503"));
        assertFalse(body.toString().contains("test dependency failure"));
        verify(jwt, never()).invalidateAllTokens(any(), any());
        assertEquals(1, logs.list.size());
        assertTrue(logs.list.get(0).getFormattedMessage().startsWith("event=front_auth_unavailable "));
    }

    @Test
    void slowUnauthorizedRequestIsLoggedWithoutAfterCompletion() throws Exception {
        simulateSlowRequest();
        when(request.getHeader("Authorization")).thenReturn("Bearer private-token");
        when(request.getQueryString()).thenReturn("secret=sensitive-query");
        when(response.getStatus()).thenReturn(401);

        assertFalse(interceptor.preHandle(request, response, new Object()));

        verify(response).setStatus(401);
        assertTrue(body.toString().contains("\"code\":401"));
        assertEquals(1, logs.list.size());
        String message = logs.list.get(0).getFormattedMessage();
        assertTrue(message.startsWith("event=front_api_slow path=/api/order status=401 "));
        assertFalse(message.contains("private-token"));
        assertFalse(message.contains("sensitive-query"));
    }

    @Test
    void slowAuthenticatedRequestIsLoggedOnceAfterCompletion() throws Exception {
        simulateSlowRequest();
        when(jwt.authenticate("probe")).thenReturn(
                new FrontJwtUtil.FrontPrincipal(1L, "member_1", "session", false));
        when(response.getStatus()).thenReturn(200);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertTrue(logs.list.isEmpty());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertEquals(1, logs.list.size());
        assertTrue(logs.list.get(0).getFormattedMessage()
                .startsWith("event=front_api_slow path=/api/order status=200 "));
    }

    private void simulateSlowRequest() {
        // The mocked request returns a controlled start time; no wall-clock sleep is needed.
        when(request.getAttribute(START_TIME))
                .thenReturn(System.nanoTime() - TimeUnit.SECONDS.toNanos(2));
    }
}
