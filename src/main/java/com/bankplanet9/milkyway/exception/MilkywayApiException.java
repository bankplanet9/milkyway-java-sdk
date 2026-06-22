package com.bankplanet9.milkyway.exception;

/**
 * Base exception for all errors returned by the MilkyWay Payments API. Carries the
 * HTTP status code and the server's {@code error} message when present.
 */
public class MilkywayApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** HTTP status code returned by the API (0 if the request never completed). */
    private final int statusCode;

    /** Raw response body, useful for diagnostics when no structured error was parsed. */
    private final String responseBody;

    public MilkywayApiException(int statusCode, String message, String responseBody) {
        this(statusCode, message, responseBody, null);
    }

    public MilkywayApiException(int statusCode, String message, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
