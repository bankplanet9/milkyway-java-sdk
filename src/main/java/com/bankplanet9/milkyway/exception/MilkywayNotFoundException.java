package com.bankplanet9.milkyway.exception;

/** HTTP 404 — transaction not found, or not owned by your institution. */
public final class MilkywayNotFoundException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayNotFoundException(String message, String responseBody) {
        super(404, message, responseBody);
    }
}
