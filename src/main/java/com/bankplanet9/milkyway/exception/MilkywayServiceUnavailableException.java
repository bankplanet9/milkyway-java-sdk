package com.bankplanet9.milkyway.exception;

/** HTTP 5xx — the API or a downstream recipient service is unavailable. */
public final class MilkywayServiceUnavailableException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayServiceUnavailableException(int statusCode, String message, String responseBody) {
        super(statusCode, message, responseBody);
    }
}
