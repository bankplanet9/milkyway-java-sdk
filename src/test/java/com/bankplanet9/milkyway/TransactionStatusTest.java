package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.model.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionStatusTest {

    @Test
    void codesMatchSpec() {
        assertEquals(0, TransactionStatus.PENDING.code());
        assertEquals(1, TransactionStatus.DONE.code());
        assertEquals(2, TransactionStatus.CANCELLED.code());
        assertEquals(3, TransactionStatus.FAILED.code());
        assertEquals(4, TransactionStatus.CANCEL_PENDING.code());
        assertEquals(5, TransactionStatus.STUCK.code());
    }

    @Test
    void terminalSet() {
        assertTrue(TransactionStatus.DONE.isTerminal());
        assertTrue(TransactionStatus.CANCELLED.isTerminal());
        assertTrue(TransactionStatus.FAILED.isTerminal());
        assertTrue(TransactionStatus.STUCK.isTerminal());
        assertFalse(TransactionStatus.PENDING.isTerminal());
        assertFalse(TransactionStatus.CANCEL_PENDING.isTerminal());
    }

    @Test
    void fromCodeRoundTrips() {
        for (TransactionStatus s : TransactionStatus.values()) {
            assertEquals(s, TransactionStatus.fromCode(s.code()));
        }
    }

    @Test
    void fromUnknownCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromCode(99));
    }
}
