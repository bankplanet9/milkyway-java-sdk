package com.bankplanet9.milkyway.exception;

/** HTTP 400 — request validation failed (bad amount, missing field, unresolvable FX rate, ...). */
public final class MilkywayValidationException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayValidationException(String message, String responseBody) {
        super(400, message, responseBody);
    }
}
