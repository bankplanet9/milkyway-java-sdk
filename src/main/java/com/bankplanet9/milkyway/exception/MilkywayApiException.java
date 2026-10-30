package com.bankplanet9.milkyway.exception;

/**
 * Base exception for all errors returned by the MilkyWay Payments API. Carries the
 * HTTP status code, the server's {@code error} message and, when that message starts
 * with one, its machine-readable code (for example {@code ErrPaymentRejected}).
 */
public class MilkywayApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** HTTP status code returned by the API (0 if the request never completed). */
    private final int statusCode;

    /** Raw response body, useful for diagnostics when no structured error was parsed. */
    private final String responseBody;

    /** The leading {@code Err...} code of the server's {@code error}, or {@code null}. */
    private final String errorCode;

    public MilkywayApiException(int statusCode, String message, String responseBody) {
        this(statusCode, message, responseBody, null, null);
    }

    public MilkywayApiException(int statusCode, String message, String responseBody, Throwable cause) {
        this(statusCode, message, responseBody, null, cause);
    }

    public MilkywayApiException(int statusCode, String message, String responseBody, String errorCode,
                                Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    /**
     * The machine-readable code that leads the server's {@code error} — the whole
     * value when it is a bare code ({@code ErrPaymentRejected}), or the part before the
     * first {@code ':'} when details follow ({@code ErrRecipientRateUnavailable: ...}).
     * {@code null} when the {@code error} does not start with an {@code Err...} code or
     * the response carried none. Compare it with the codes the API documents rather
     * than parsing {@link #getMessage()}.
     */
    public String getErrorCode() {
        return errorCode;
    }
}
