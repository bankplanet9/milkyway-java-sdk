package com.bankplanet9.milkyway.exception;

/**
 * HTTP 400 — request validation failed (bad amount, missing field, unresolvable FX
 * rate, a recipient the receiving bank rejects as invalid, ...). When the failure
 * concerns one request field, {@link #getField()} names it.
 */
public final class MilkywayValidationException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public MilkywayValidationException(String message, String responseBody) {
        this(message, responseBody, null, null);
    }

    public MilkywayValidationException(String message, String responseBody, String errorCode, String field) {
        super(400, message, responseBody, errorCode, null);
        this.field = field;
    }

    /**
     * The JSON name (or path) of the request field at fault — for example
     * {@code amount_credit}, {@code currency_credit}, {@code recipient_id} (the
     * receiving bank reported the recipient invalid) or
     * {@code sender_info.date_of_birth} — or {@code null} when the error is not about
     * one field.
     */
    public String getField() {
        return field;
    }
}
