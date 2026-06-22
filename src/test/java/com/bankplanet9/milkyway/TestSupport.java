package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.auth.AccessTokenProvider;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared helpers for the test suite. */
final class TestSupport {

    private TestSupport() {
    }

    static MilkywayOptions.Builder optionsFor(String baseUrl) {
        return MilkywayOptions.builder()
                .baseUrl(baseUrl)
                .tokenUrl(baseUrl + "/token")
                .clientId("test-client")
                .clientSecret("test-secret")
                .requestTimeout(Duration.ofSeconds(5))
                .retryBaseDelay(Duration.ofMillis(1))
                .maxRetries(3);
    }

    /** A token provider that never hits the network; counts force-refreshes. */
    static final class FakeTokenProvider implements AccessTokenProvider {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger forceRefreshes = new AtomicInteger();
        private final String token;

        FakeTokenProvider(String token) {
            this.token = token;
        }

        @Override
        public String getAccessToken(boolean forceRefresh) {
            calls.incrementAndGet();
            if (forceRefresh) {
                forceRefreshes.incrementAndGet();
            }
            return token + (forceRefresh ? "-refreshed" : "");
        }
    }
}
