package com.bankplanet9.milkyway;

import java.time.Duration;

/** Controls {@link MilkywayPaymentsClient#waitForCompletion} polling. */
public final class PollOptions {

    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Duration timeout;

    private PollOptions(Builder b) {
        this.initialDelay = b.initialDelay;
        this.maxDelay = b.maxDelay;
        this.backoffMultiplier = b.backoffMultiplier;
        this.timeout = b.timeout;
    }

    /** Default polling options. */
    public static PollOptions defaults() {
        return builder().build();
    }

    /** Delay before the first poll. Default 1s. */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /** Maximum delay between polls (backoff is capped here). Default 30s. */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /** Multiplier applied to the delay after each poll. Default 2.0. */
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    /**
     * Overall budget for polling. When exceeded, the last observed status is
     * returned even if non-terminal. Default 5 minutes.
     */
    public Duration getTimeout() {
        return timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PollOptions}. */
    public static final class Builder {
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffMultiplier = 2.0;
        private Duration timeout = Duration.ofMinutes(5);

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public PollOptions build() {
            return new PollOptions(this);
        }
    }
}
