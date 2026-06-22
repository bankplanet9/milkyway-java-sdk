package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Result of a cancellation request: the resulting transaction status. */
public final class CancelResult {

    @JsonProperty("status")
    private TransactionStatus status;

    public TransactionStatus getStatus() {
        return status;
    }
}
