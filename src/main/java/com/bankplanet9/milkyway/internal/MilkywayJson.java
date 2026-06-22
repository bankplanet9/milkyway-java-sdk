package com.bankplanet9.milkyway.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared {@link ObjectMapper} for the SDK.
 *
 * <p>Money fields bind to {@link java.math.BigDecimal} losslessly: incoming JSON
 * numbers are read with {@link DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS}
 * so no binary-float rounding ever happens, and on the way out a {@code BigDecimal}
 * is serialised as a bare JSON number (Jackson's default). The transaction status
 * enum is encoded as its integer code via {@code @JsonValue} / {@code @JsonCreator}.
 */
public final class MilkywayJson {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private MilkywayJson() {
    }
}
