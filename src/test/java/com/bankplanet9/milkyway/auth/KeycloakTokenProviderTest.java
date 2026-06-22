package com.bankplanet9.milkyway.auth;

import com.bankplanet9.milkyway.MilkywayOptions;
import com.bankplanet9.milkyway.exception.MilkywayAuthException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakTokenProviderTest {

    private HttpServer server;
    private final AtomicInteger tokenCalls = new AtomicInteger();
    private final AtomicReference<String> lastForm = new AtomicReference<>();
    private volatile String responseBody = "{\"access_token\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":300}";
    private volatile int responseStatus = 200;
    private volatile CountDownLatch holdLatch; // optional gate to test single-flight

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenCalls.incrementAndGet();
            byte[] in = exchange.getRequestBody().readAllBytes();
            lastForm.set(new String(in, StandardCharsets.UTF_8));
            CountDownLatch latch = holdLatch;
            if (latch != null) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private MilkywayOptions options() {
        return MilkywayOptions.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .tokenUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/token")
                .clientId("cid")
                .clientSecret("secret")
                .tokenRefreshSkew(Duration.ofSeconds(30))
                .build();
    }

    @Test
    void cachesTokenAndSendsClientCredentialsForm() {
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());

        assertEquals("abc", provider.getAccessToken(false));
        assertEquals("abc", provider.getAccessToken(false));

        assertEquals(1, tokenCalls.get(), "second call must hit the cache");
        String form = lastForm.get();
        assertTrue(form.contains("grant_type=client_credentials"));
        assertTrue(form.contains("client_id=cid"));
        assertTrue(form.contains("client_secret=secret"));
    }

    @Test
    void includesScopeWhenSet() {
        MilkywayOptions opts = MilkywayOptions.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .tokenUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/token")
                .clientId("cid")
                .clientSecret("secret")
                .scope("payments")
                .build();
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), opts);

        provider.getAccessToken(false);

        assertTrue(lastForm.get().contains("scope=payments"));
    }

    @Test
    void refreshesWhenWithinSkewOfExpiry() {
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        provider.setClock(now::get);

        provider.getAccessToken(false); // expires_in=300 -> expiry at +300s
        // Advance to +280s: within the 30s skew window -> stale -> refetch.
        now.set(Instant.parse("2026-01-01T00:04:40Z"));
        provider.getAccessToken(false);

        assertEquals(2, tokenCalls.get());
    }

    @Test
    void forceRefreshBypassesCache() {
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());

        provider.getAccessToken(false);
        provider.getAccessToken(true);

        assertEquals(2, tokenCalls.get());
    }

    @Test
    void singleFlightCollapsesConcurrentAcquisitions() throws Exception {
        holdLatch = new CountDownLatch(1);
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        Future<?>[] futures = new Future<?>[8];
        for (int i = 0; i < 8; i++) {
            futures[i] = pool.submit(() -> {
                ready.countDown();
                return provider.getAccessToken(false);
            });
        }
        ready.await();
        Thread.sleep(100); // let callers queue on the lock
        holdLatch.countDown();
        for (Future<?> f : futures) {
            assertEquals("abc", f.get());
        }
        pool.shutdown();

        assertEquals(1, tokenCalls.get(), "concurrent callers must trigger exactly one fetch");
    }

    @Test
    void nonSuccessStatusRaisesAuthException() {
        responseStatus = 401;
        responseBody = "{\"error\":\"invalid_client\"}";
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());

        MilkywayAuthException ex = assertThrows(MilkywayAuthException.class,
                () -> provider.getAccessToken(false));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void missingAccessTokenRaisesAuthException() {
        responseBody = "{\"token_type\":\"Bearer\",\"expires_in\":300}";
        KeycloakTokenProvider provider = new KeycloakTokenProvider(HttpClient.newHttpClient(), options());

        assertThrows(MilkywayAuthException.class, () -> provider.getAccessToken(false));
    }
}
