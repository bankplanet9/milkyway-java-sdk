package com.bankplanet9.milkyway.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire request body for {@code POST /payments/v1/cancel}. */
public final class CancelRequest {

    @JsonProperty("transaction_id")
    private final long transactionId;

    public CancelRequest(long transactionId) {
        this.transactionId = transactionId;
    }

    public long getTransactionId() {
        return transactionId;
    }
}
