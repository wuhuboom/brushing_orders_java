package com.order.framework.front;

/** A temporary session-store or account-database failure, rather than bad credentials. */
public class FrontAuthenticationUnavailableException extends RuntimeException {
    public FrontAuthenticationUnavailableException(Throwable cause) {
        super("Member authentication is temporarily unavailable", cause);
    }
}
