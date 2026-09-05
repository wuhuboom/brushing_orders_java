package com.order.framework.web.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest
{
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingResourceKeepsTheRealHttp404Status()
    {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/profile/avatar/missing.png");
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET, "avatar/missing.png");

        ResponseEntity<Void> response = handler.handleNoResourceFoundException(exception, request);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void clientAbortIsHandledWithoutWritingAnotherResponseBody()
    {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/profile/upload/large.jpg");

        assertDoesNotThrow(() -> handler.handleClientAbortException(
                new ClientAbortException("Connection reset by peer"), request));
    }
}
