package com.bankplanet9.milkyway;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MilkywayOptionsTest {

    @Test
    void appliesSensibleDefaults() {
        MilkywayOptions o = MilkywayOptions.builder()
                .baseUrl("https://api")
                .tokenUrl("https://token")
                .clientId("id")
                .clientSecret("secret")
                .build();

        assertEquals(3, o.getMaxRetries());
        assertEquals(Duration.ofMillis(500), o.getRetryBaseDelay());
        assertEquals(Duration.ofSeconds(30), o.getRequestTimeout());
        assertEquals(Duration.ofSeconds(30), o.getTokenRefreshSkew());
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> MilkywayOptions.builder()
                .tokenUrl("https://token").clientId("id").clientSecret("secret").build());
        assertThrows(IllegalArgumentException.class, () -> MilkywayOptions.builder()
                .baseUrl("https://api").clientId("id").clientSecret("secret").build());
        assertThrows(IllegalArgumentException.class, () -> MilkywayOptions.builder()
                .baseUrl("https://api").tokenUrl("https://token").clientSecret("secret").build());
        assertThrows(IllegalArgumentException.class, () -> MilkywayOptions.builder()
                .baseUrl("https://api").tokenUrl("https://token").clientId("id").build());
    }

    @Test
    void rejectsNegativeMaxRetries() {
        assertThrows(IllegalArgumentException.class, () -> MilkywayOptions.builder()
                .baseUrl("https://api").tokenUrl("https://token")
                .clientId("id").clientSecret("secret").maxRetries(-1).build());
    }
}
