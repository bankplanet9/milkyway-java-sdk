package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status of a payment transaction, mirroring the integer codes returned
 * by the Payments API ({@code postcheck} / {@code cancel}).
 */
public enum TransactionStatus {

    /** Accepted, not yet completed. Keep polling. */
    PENDING(0),

    /** Completed successfully (terminal). */
    DONE(1),

    /** Cancelled (terminal). */
    CANCELLED(2),

    /** Failed (terminal). Inspect the accompanying error message. */
    FAILED(3),

    /** Cancellation requested, awaiting confirmation. */
    CANCEL_PENDING(4),

    /** Timed out / stuck (terminal); requires operator attention. */
    STUCK(5);

    private final int code;

    TransactionStatus(int code) {
        this.code = code;
    }

    /** The integer code as encoded on the wire. */
    @JsonValue
    public int code() {
        return code;
    }

    /**
     * True when the status will not change without external action — {@link #DONE},
     * {@link #CANCELLED}, {@link #FAILED}, or {@link #STUCK}. Polling should stop
     * once a terminal status is reached.
     */
    public boolean isTerminal() {
        switch (this) {
            case DONE:
            case CANCELLED:
            case FAILED:
            case STUCK:
                return true;
            default:
                return false;
        }
    }

    /** Maps an integer wire code to its enum value. */
    @JsonCreator
    public static TransactionStatus fromCode(int code) {
        for (TransactionStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
