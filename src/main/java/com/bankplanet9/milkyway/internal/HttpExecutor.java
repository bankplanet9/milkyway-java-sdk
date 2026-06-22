package com.bankplanet9.milkyway.internal;

import com.bankplanet9.milkyway.MilkywayOptions;
import com.bankplanet9.milkyway.auth.AccessTokenProvider;
import com.bankplanet9.milkyway.exception.MilkywayApiException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sends API requests through the resilience + auth pipeline:
 *
 * <ul>
 *   <li>attaches the bearer token (cached, single-flight via the token provider);</li>
 *   <li>retries transient failures (HTTP 5xx, 408, and {@link IOException}) with
 *       exponential backoff + jitter, up to {@code maxRetries}; deterministic
 *       outcomes (400/401/402/404) are never retried;</li>
 *   <li>on a {@code 401}, force-refreshes the token once and replays the request;</li>
 *   <li>enforces a per-attempt request timeout via the {@link HttpRequest} timeout.</li>
 * </ul>
 *
 * <p>A request marked non-retryable (a {@code pay} without an Idempotency-Key)
 * bypasses retries entirely to avoid duplicate side effects, but still gets the
 * per-attempt timeout and the single 401 replay.
 */
public final class HttpExecutor {

    private final HttpClient httpClient;
    private final AccessTokenProvider tokenProvider;
    private final MilkywayOptions options;

    public HttpExecutor(HttpClient httpClient, AccessTokenProvider tokenProvider, MilkywayOptions options) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.options = options;
    }

    /**
     * Executes a request.
     *
     * @param builder  a fresh request builder per call site (without Authorization or
     *                 per-attempt timeout — both are applied here so each attempt is
     *                 independent).
     * @param retryable whether transient failures may be retried.
     */
    public HttpResponse<String> send(HttpRequest.Builder builder, boolean retryable) {
        int maxAttempts = retryable ? options.getMaxRetries() + 1 : 1;
        IOException lastIo = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            HttpResponse<String> response;
            try {
                response = sendOnceWithAuthReplay(builder);
            } catch (IOException ex) {
                lastIo = ex;
                if (attempt < maxAttempts - 1) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new MilkywayServiceUnavailableMarker(ex);
            }

            if (retryable && isTransient(response.statusCode()) && attempt < maxAttempts - 1) {
                sleepBackoff(attempt);
                continue;
            }
            return response;
        }

        // Unreachable: the loop either returns or throws. Guard for completeness.
        throw new MilkywayServiceUnavailableMarker(lastIo);
    }

    private HttpResponse<String> sendOnceWithAuthReplay(HttpRequest.Builder builder) throws IOException {
        HttpResponse<String> response = sendOnce(builder, false);
        if (response.statusCode() != 401) {
            return response;
        }
        // Token may be stale/revoked: refresh once and replay.
        return sendOnce(builder, true);
    }

    private HttpResponse<String> sendOnce(HttpRequest.Builder builder, boolean forceRefresh) throws IOException {
        String token = tokenProvider.getAccessToken(forceRefresh);
        HttpRequest request = builder.copy()
                .timeout(options.getRequestTimeout())
                .header("Authorization", "Bearer " + token)
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MilkywayApiException(0, "Interrupted while sending request.", null, ex);
        }
    }

    private void sleepBackoff(int attempt) {
        double base = options.getRetryBaseDelay().toMillis();
        // Exponential backoff: base * 2^attempt, plus full jitter in [0, computed].
        double exp = base * Math.pow(2, attempt);
        long jittered = (long) (ThreadLocalRandom.current().nextDouble() * exp);
        if (jittered <= 0) {
            return;
        }
        try {
            Thread.sleep(jittered);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MilkywayApiException(0, "Interrupted during retry backoff.", null, ex);
        }
    }

    private static boolean isTransient(int status) {
        return status >= 500 || status == 408;
    }

    /** Internal carrier so the client can map a terminal network failure to a 5xx-style exception. */
    public static final class MilkywayServiceUnavailableMarker extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MilkywayServiceUnavailableMarker(IOException cause) {
            super(cause == null ? "Network failure" : cause.getMessage(), cause);
        }
    }
}
