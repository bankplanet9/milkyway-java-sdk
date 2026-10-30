package com.bankplanet9.milkyway.model;

/**
 * The reason codes {@link PostcheckResult#getError()} carries for a terminal outcome
 * the receiving bank gave a reason for. The value is always one of these codes, never
 * the bank's own text; compare with {@code equals}. New codes may be added, so treat
 * an unknown value as a generic failure.
 */
public final class StatusReason {

    /** The receiving bank declined the payment (also any reason it gave without a code). */
    public static final String PAYMENT_REJECTED = "ErrPaymentRejected";

    /** The receiving bank does not know the recipient. */
    public static final String RECIPIENT_NOT_FOUND = "ErrRecipientNotFound";

    /** The receiving bank considers the recipient identifier invalid. */
    public static final String RECIPIENT_INVALID = "ErrRecipientInvalid";

    /** The receiving bank's service was unavailable. */
    public static final String SERVICE_UNAVAILABLE = "ErrServiceUnavailable";

    /** The receiving bank refused to cancel the payment. */
    public static final String PAYMENT_CANNOT_BE_CANCELED = "ErrPaymentCannotBeCanceled";

    private StatusReason() {
    }
}
