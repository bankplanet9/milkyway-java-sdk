package com.bankplanet9.milkyway.auth;

import com.bankplanet9.milkyway.MilkywayOptions;
import com.bankplanet9.milkyway.exception.MilkywayAuthException;
import com.bankplanet9.milkyway.internal.MilkywayJson;
import com.bankplanet9.milkyway.internal.TokenResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Acquires access tokens via the Keycloak client-credentials grant and caches them
 * until shortly before expiry. Concurrent callers share a single in-flight
 * acquisition (single-flight via a {@link ReentrantLock}) so a burst of requests
 * triggers at most one token fetch.
 *
 * <p>The supplied {@link HttpClient} is dedicated to the token endpoint and must
 * NOT route through the SDK's auth/retry pipeline, to avoid recursion.
 */
public final class KeycloakTokenProvider implements AccessTokenProvider {

    private final HttpClient httpClient;
    private final MilkywayOptions options;
    private final ReentrantLock lock = new ReentrantLock();

    /** Clock seam for tests. Defaults to wall-clock UTC. */
    private Supplier<Instant> clock = Instant::now;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.MIN;

    public KeycloakTokenProvider(HttpClient httpClient, MilkywayOptions options) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient is required.");
        }
        if (options == null) {
            throw new IllegalArgumentException("options is required.");
        }
        this.httpClient = httpClient;
        this.options = options;
    }

    /** Test seam: override the wall clock used for cache expiry. */
    void setClock(Supplier<Instant> clock) {
        this.clock = clock;
    }

    @Override
    public String getAccessToken(boolean forceRefresh) {
        if (!forceRefresh) {
            String cached = tryGetCached();
            if (cached != null) {
                return cached;
            }
        }

        lock.lock();
        try {
            // Re-check under the lock: another caller may have refreshed while we waited.
            if (!forceRefresh) {
                String cached = tryGetCached();
                if (cached != null) {
                    return cached;
                }
            }
            return acquire();
        } finally {
            lock.unlock();
        }
    }

    private String tryGetCached() {
        String token = cachedToken;
        if (token != null && clock.get().isBefore(expiresAt.minus(options.getTokenRefreshSkew()))) {
            return token;
        }
        return null;
    }

    private String acquire() {
        String form = buildForm();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(options.getTokenUrl()))
                .timeout(options.getRequestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new MilkywayAuthException("Failed to reach the Keycloak token endpoint.", null, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MilkywayAuthException("Interrupted while acquiring access token.", null, ex);
        }

        String body = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new MilkywayAuthException(
                    "Token acquisition failed with status " + response.statusCode() + ".", body);
        }

        TokenResponse parsed;
        try {
            parsed = MilkywayJson.MAPPER.readValue(body, TokenResponse.class);
        } catch (IOException ex) {
            throw new MilkywayAuthException("Token endpoint returned a malformed response.", body, ex);
        }

        if (parsed == null || parsed.getAccessToken() == null || parsed.getAccessToken().isEmpty()) {
            throw new MilkywayAuthException("Token endpoint response contained no access_token.", body);
        }

        cachedToken = parsed.getAccessToken();
        expiresAt = clock.get().plus(Duration.ofSeconds(parsed.getExpiresIn()));
        return cachedToken;
    }

    private String buildForm() {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[]{"grant_type", "client_credentials"});
        pairs.add(new String[]{"client_id", options.getClientId()});
        pairs.add(new String[]{"client_secret", options.getClientSecret()});
        String scope = options.getScope();
        if (scope != null && !scope.trim().isEmpty()) {
            pairs.add(new String[]{"scope", scope});
        }

        StringBuilder sb = new StringBuilder();
        for (String[] pair : pairs) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(pair[0])).append('=').append(encode(pair[1]));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
