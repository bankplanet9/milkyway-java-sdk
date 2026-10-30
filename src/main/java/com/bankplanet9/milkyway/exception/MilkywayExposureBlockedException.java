package com.bankplanet9.milkyway.exception;

/**
 * HTTP 402 — rejected because the payment would breach a block-action exposure
 * limit (effective limit = configured limit + funded deposit balance).
 */
public final class MilkywayExposureBlockedException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayExposureBlockedException(String message, String responseBody) {
        this(message, responseBody, null);
    }

    public MilkywayExposureBlockedException(String message, String responseBody, String errorCode) {
        super(402, message, responseBody, errorCode, null);
    }
}
