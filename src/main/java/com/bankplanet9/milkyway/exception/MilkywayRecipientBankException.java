package com.bankplanet9.milkyway.exception;

/**
 * HTTP 502 — the receiving bank's connector failed in a way MilkyWay could not
 * classify; {@link #getErrorCode()} is {@code ErrRecipientBankError}. Nothing was
 * created or debited, and MilkyWay is alerted. Retry later or route the payment
 * through another service.
 *
 * <p>It extends {@link MilkywayServiceUnavailableException}, so existing
 * {@code catch (MilkywayServiceUnavailableException e)} blocks still catch it, and
 * it is retried like any other transient 5xx.
 */
public final class MilkywayRecipientBankException extends MilkywayServiceUnavailableException {

    private static final long serialVersionUID = 1L;

    public MilkywayRecipientBankException(String message, String responseBody, String errorCode) {
        super(502, message, responseBody, errorCode, null, null);
    }
}
