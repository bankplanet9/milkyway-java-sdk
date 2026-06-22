package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.model.PostcheckResult;
import com.bankplanet9.milkyway.model.TransactionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitForCompletionTest {

    private StubServer server;
    private MilkywayPaymentsClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubServer();
        MilkywayOptions options = TestSupport.optionsFor(server.baseUrl()).build();
        client = new MilkywayPaymentsClient(options, HttpClient.newHttpClient(),
                new TestSupport.FakeTokenProvider("tok"));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void pollsUntilTerminal() {
        server.onPathSequence("/payments/v1/postcheck", List.of(
                new StubServer.Response(200, "{\"transaction_id\":1,\"status\":0}"),
                new StubServer.Response(200, "{\"transaction_id\":1,\"status\":0}"),
                new StubServer.Response(200, "{\"transaction_id\":1,\"status\":1}")));

        PollOptions poll = PollOptions.builder()
                .initialDelay(Duration.ofMillis(5))
                .maxDelay(Duration.ofMillis(20))
                .backoffMultiplier(2.0)
                .timeout(Duration.ofSeconds(10))
                .build();

        PostcheckResult result = client.waitForCompletion(1, poll);

        assertEquals(TransactionStatus.DONE, result.getStatus());
        assertEquals(3, server.hits("/payments/v1/postcheck"));
    }

    @Test
    void returnsLastNonTerminalWhenBudgetExhausted() {
        server.onPath("/payments/v1/postcheck",
                c -> new StubServer.Response(200, "{\"transaction_id\":1,\"status\":0}"));

        PollOptions poll = PollOptions.builder()
                .initialDelay(Duration.ofMillis(50))
                .maxDelay(Duration.ofMillis(50))
                .timeout(Duration.ofMillis(10)) // smaller than first delay -> bail after first poll
                .build();

        PostcheckResult result = client.waitForCompletion(1, poll);

        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals(1, server.hits("/payments/v1/postcheck"));
    }

    @Test
    void terminalOnFirstPollDoesNotSleep() {
        server.onPath("/payments/v1/postcheck",
                c -> new StubServer.Response(200, "{\"transaction_id\":1,\"status\":3,\"error\":\"x\"}"));

        long start = System.nanoTime();
        PostcheckResult result = client.waitForCompletion(1);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals(1, server.hits("/payments/v1/postcheck"));
        assertTrue(elapsedMs < 1000, "should return immediately on a terminal first poll");
    }
}
