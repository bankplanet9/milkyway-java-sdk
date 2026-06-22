package com.bankplanet9.milkyway.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire response of a successful {@code pay} call. */
public final class PayResponse {

    @JsonProperty("transaction_id")
    private long transactionId;

    public long getTransactionId() {
        return transactionId;
    }
}
