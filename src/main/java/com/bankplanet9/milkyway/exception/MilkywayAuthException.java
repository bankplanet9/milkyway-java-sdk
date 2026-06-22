package com.bankplanet9.milkyway.exception;

/** HTTP 401 — missing, malformed, or invalid access token (incl. failed token acquisition). */
public final class MilkywayAuthException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayAuthException(String message, String responseBody) {
        super(401, message, responseBody);
    }

    public MilkywayAuthException(String message, String responseBody, Throwable cause) {
        super(401, message, responseBody, cause);
    }
}
