package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Current status of a payment transaction. */
public final class PostcheckResult {

    @JsonProperty("transaction_id")
    private long transactionId;

    @JsonProperty("status")
    private TransactionStatus status;

    /**
     * The receiving bank's reason code for a terminal outcome, when it gave one — one of
     * the {@link StatusReason} constants (e.g. {@link StatusReason#PAYMENT_REJECTED}),
     * never the bank's own text. {@code null} when no reason was given.
     */
    @JsonProperty("error")
    private String error;

    public long getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    /** @return a {@link StatusReason} code, or {@code null}. */
    public String getError() {
        return error;
    }
}
