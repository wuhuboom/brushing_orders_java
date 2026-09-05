package com.order.common.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionUtilTest
{
    @Test
    void conciseMessageUsesDeepestCauseAndRemovesLineBreaks()
    {
        RuntimeException exception = new RuntimeException(
                "request failed",
                new IOException("Connection reset\r\nby peer"));

        String message = ExceptionUtil.getConciseErrorMessage(exception);

        assertEquals("IOException: Connection reset by peer", message);
        assertFalse(message.contains("\n"));
        assertFalse(message.contains("\r"));
    }

    @Test
    void conciseMessageLimitsUntrustedMessageLength()
    {
        String message = ExceptionUtil.getConciseErrorMessage(
                new IllegalStateException("x".repeat(1200)));

        assertTrue(message.startsWith("IllegalStateException: "));
        assertTrue(message.endsWith("..."));
        assertTrue(message.length() < 1050);
    }
}
