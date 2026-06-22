package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Current status of a payment transaction. */
public final class PostcheckResult {

    @JsonProperty("transaction_id")
    private long transactionId;

    @JsonProperty("status")
    private TransactionStatus status;

    /** Failure detail; present only when the payment failed. */
    @JsonProperty("error")
    private String error;

    public long getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
