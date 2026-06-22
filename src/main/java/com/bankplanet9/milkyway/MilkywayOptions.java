package com.bankplanet9.milkyway;

import java.time.Duration;

/**
 * Configuration for {@link MilkywayPaymentsClient}: API endpoint, Keycloak
 * client-credentials, and resilience tuning. Build via {@link #builder()}.
 */
public final class MilkywayOptions {

    private final String baseUrl;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final Duration tokenRefreshSkew;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration retryBaseDelay;

    private MilkywayOptions(Builder b) {
        this.baseUrl = b.baseUrl;
        this.tokenUrl = b.tokenUrl;
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.scope = b.scope;
        this.tokenRefreshSkew = b.tokenRefreshSkew;
        this.requestTimeout = b.requestTimeout;
        this.maxRetries = b.maxRetries;
        this.retryBaseDelay = b.retryBaseDelay;
        validate();
    }

    private void validate() {
        if (isBlank(baseUrl)) {
            throw new IllegalArgumentException("baseUrl is required.");
        }
        if (isBlank(tokenUrl)) {
            throw new IllegalArgumentException("tokenUrl is required.");
        }
        if (isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is required.");
        }
        if (isBlank(clientSecret)) {
            throw new IllegalArgumentException("clientSecret is required.");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative.");
        }
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive.");
        }
        if (retryBaseDelay == null || retryBaseDelay.isNegative()) {
            throw new IllegalArgumentException("retryBaseDelay cannot be negative.");
        }
        if (tokenRefreshSkew == null || tokenRefreshSkew.isNegative()) {
            throw new IllegalArgumentException("tokenRefreshSkew cannot be negative.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Base URL of the Payments API, e.g. {@code https://milkyway.stage.planet9.ae}. */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** Keycloak token endpoint for the client-credentials grant. */
    public String getTokenUrl() {
        return tokenUrl;
    }

    /** Keycloak client id issued to your institution (becomes the {@code azp} claim). */
    public String getClientId() {
        return clientId;
    }

    /** Keycloak client secret issued to your institution. */
    public String getClientSecret() {
        return clientSecret;
    }

    /** Optional OAuth scope to request. {@code null} by default. */
    public String getScope() {
        return scope;
    }

    /** How long before a token's stated expiry it is refreshed. Default 30s. */
    public Duration getTokenRefreshSkew() {
        return tokenRefreshSkew;
    }

    /** Per-attempt request timeout. Default 30s. */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /** Maximum automatic retries for transient failures (5xx, 408, network). Default 3. */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** Base delay for exponential backoff between retries. Default 500ms. */
    public Duration getRetryBaseDelay() {
        return retryBaseDelay;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link MilkywayOptions} with sensible resilience defaults. */
    public static final class Builder {
        private String baseUrl;
        private String tokenUrl;
        private String clientId;
        private String clientSecret;
        private String scope;
        private Duration tokenRefreshSkew = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private Duration retryBaseDelay = Duration.ofMillis(500);

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder tokenRefreshSkew(Duration tokenRefreshSkew) {
            this.tokenRefreshSkew = tokenRefreshSkew;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryBaseDelay(Duration retryBaseDelay) {
            this.retryBaseDelay = retryBaseDelay;
            return this;
        }

        public MilkywayOptions build() {
            return new MilkywayOptions(this);
        }
    }
}
