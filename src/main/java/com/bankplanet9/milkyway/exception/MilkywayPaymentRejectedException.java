package com.bankplanet9.milkyway.exception;

/**
 * HTTP 422 — the receiving bank refused the payment on its merits (for example a
 * limit or a blocked account); {@link #getErrorCode()} is {@code ErrPaymentRejected}.
 * The request was well formed, but repeating it will not change the answer, so the
 * SDK never retries it. Nothing was created or debited. The bank's own reason is not
 * disclosed.
 */
public final class MilkywayPaymentRejectedException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    public MilkywayPaymentRejectedException(String message, String responseBody, String errorCode) {
        super(422, message, responseBody, errorCode, null);
    }
}
