package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.exception.MilkywayApiException;
import com.bankplanet9.milkyway.exception.MilkywayAuthException;
import com.bankplanet9.milkyway.exception.MilkywayExposureBlockedException;
import com.bankplanet9.milkyway.exception.MilkywayNotFoundException;
import com.bankplanet9.milkyway.exception.MilkywayServiceUnavailableException;
import com.bankplanet9.milkyway.exception.MilkywayValidationException;
import com.bankplanet9.milkyway.model.CancelResult;
import com.bankplanet9.milkyway.model.PayRequest;
import com.bankplanet9.milkyway.model.PostcheckResult;
import com.bankplanet9.milkyway.model.PrecheckRequest;
import com.bankplanet9.milkyway.model.PrecheckResult;
import com.bankplanet9.milkyway.model.StatusReason;
import com.bankplanet9.milkyway.model.TransactionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilkywayPaymentsClientTest {

    private StubServer server;
    private TestSupport.FakeTokenProvider tokens;
    private MilkywayPaymentsClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubServer();
        tokens = new TestSupport.FakeTokenProvider("tok");
        MilkywayOptions options = TestSupport.optionsFor(server.baseUrl()).build();
        client = new MilkywayPaymentsClient(options, HttpClient.newHttpClient(), tokens);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void healthcheckReturnsBodyAndSendsQueryAndBearer() {
        server.onPath("/payments/v1/healthcheck", c -> new StubServer.Response(200, "OK", "text/plain"));

        String status = client.healthcheck("bank-a", "card-payout");

        assertEquals("OK", status);
        StubServer.Captured req = server.captured().get(0);
        assertEquals("GET", req.method);
        assertTrue(req.query.contains("third_party_id=bank-a"));
        assertTrue(req.query.contains("service_id=card-payout"));
        assertEquals("Bearer tok", req.header("Authorization"));
    }

    @Test
    void healthcheck500MapsToServiceUnavailable() {
        server.onPath("/payments/v1/healthcheck",
                c -> new StubServer.Response(500, "recipient down", "text/plain"));

        MilkywayServiceUnavailableException ex = assertThrows(
                MilkywayServiceUnavailableException.class,
                () -> client.healthcheck("bank-a", "svc"));
        assertEquals(500, ex.getStatusCode());
        assertEquals("recipient down", ex.getMessage());
    }

    @Test
    void precheckParsesDecimalsLosslessly() {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(200,
                "{\"third_party_id\":\"bank-a\",\"service_id\":\"svc\",\"recipient_id\":\"r1\","
                        + "\"amount_credit\":100.05,\"currency_credit\":\"USD\","
                        + "\"amount_debit\":367.0825,\"currency_debit\":\"AED\","
                        + "\"rate\":3.6729,\"commission\":1.50}"));

        PrecheckResult result = client.precheck(new PrecheckRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("100.05"))
                .setCurrencyCredit("USD"));

        assertEquals(new BigDecimal("100.05"), result.getAmountCredit());
        assertEquals(new BigDecimal("367.0825"), result.getAmountDebit());
        assertEquals(new BigDecimal("3.6729"), result.getRate());
        assertEquals(new BigDecimal("1.50"), result.getCommission());
    }

    @Test
    void precheckSerializesAmountAsBareNumberAndOmitsNullData() {
        server.onPath("/payments/v1/precheck",
                c -> new StubServer.Response(200, "{\"amount_credit\":1}"));

        client.precheck(new PrecheckRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("100.05"))
                .setCurrencyCredit("USD"));

        String body = server.captured().get(0).body;
        assertTrue(body.contains("\"amount_credit\":100.05"), "amount must be a bare JSON number: " + body);
        assertTrue(!body.contains("\"data\""), "null data must be omitted: " + body);
    }

    @Test
    void payReturnsTransactionIdAndSendsIdempotencyHeader() {
        server.onPath("/payments/v1",
                c -> new StubServer.Response(200, "{\"transaction_id\":4242}"));

        long id = client.pay(payRequest(), "idem-123");

        assertEquals(4242L, id);
        assertEquals("idem-123", server.captured().get(0).header("Idempotency-Key"));
    }

    @Test
    void payWithoutKeyOmitsIdempotencyHeader() {
        server.onPath("/payments/v1",
                c -> new StubServer.Response(200, "{\"transaction_id\":1}"));

        client.pay(payRequest());

        assertNull(server.captured().get(0).header("Idempotency-Key"));
    }

    @Test
    void postcheckMapsStatusEnumFromIntCode() {
        server.onPath("/payments/v1/postcheck",
                c -> new StubServer.Response(200, "{\"transaction_id\":7,\"status\":1}"));

        PostcheckResult result = client.postcheck(7);

        assertEquals(TransactionStatus.DONE, result.getStatus());
        assertEquals(7L, result.getTransactionId());
        assertNull(result.getError());
    }

    @Test
    void postcheckFailedCarriesError() {
        server.onPath("/payments/v1/postcheck",
                c -> new StubServer.Response(200, "{\"transaction_id\":7,\"status\":3,\"error\":\"ErrPaymentRejected\"}"));

        PostcheckResult result = client.postcheck(7);

        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals(StatusReason.PAYMENT_REJECTED, result.getError());
    }

    @Test
    void cancelMapsStatus() {
        server.onPath("/payments/v1/cancel",
                c -> new StubServer.Response(200, "{\"status\":4}"));

        CancelResult result = client.cancel(99);

        assertEquals(TransactionStatus.CANCEL_PENDING, result.getStatus());
        assertTrue(server.captured().get(0).body.contains("\"transaction_id\":99"));
    }

    @Test
    void validationErrorMapsTo400Exception() {
        server.onPath("/payments/v1/precheck",
                c -> new StubServer.Response(400, "{\"error\":\"amount must be positive\"}"));

        MilkywayValidationException ex = assertThrows(MilkywayValidationException.class,
                () -> client.precheck(minimalPrecheck()));
        assertEquals(400, ex.getStatusCode());
        assertEquals("amount must be positive", ex.getMessage());
    }

    @Test
    void authErrorMapsTo401Exception() {
        // Always 401 — even after the one forced refresh replay.
        server.onPath("/payments/v1/precheck",
                c -> new StubServer.Response(401, "{\"error\":\"invalid token\"}"));

        MilkywayAuthException ex = assertThrows(MilkywayAuthException.class,
                () -> client.precheck(minimalPrecheck()));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void exposureBlockedMapsTo402Exception() {
        server.onPath("/payments/v1",
                c -> new StubServer.Response(402, "{\"error\":\"exposure limit breached\"}"));

        MilkywayExposureBlockedException ex = assertThrows(MilkywayExposureBlockedException.class,
                () -> client.pay(payRequest(), "k"));
        assertEquals(402, ex.getStatusCode());
        assertEquals("exposure limit breached", ex.getMessage());
    }

    @Test
    void notFoundMapsTo404Exception() {
        server.onPath("/payments/v1/postcheck",
                c -> new StubServer.Response(404, "{\"error\":\"not found\"}"));

        MilkywayNotFoundException ex = assertThrows(MilkywayNotFoundException.class,
                () -> client.postcheck(123));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void retriesOn5xxThenSucceeds() {
        server.onPathSequence("/payments/v1/precheck", List.of(
                new StubServer.Response(503, "{\"error\":\"try later\"}"),
                new StubServer.Response(503, "{\"error\":\"try later\"}"),
                new StubServer.Response(200, "{\"amount_credit\":1}")));

        PrecheckResult result = client.precheck(minimalPrecheck());

        assertNotNull(result);
        assertEquals(3, server.hits("/payments/v1/precheck"));
    }

    @Test
    void doesNotRetryOn402() {
        server.onPath("/payments/v1",
                c -> new StubServer.Response(402, "{\"error\":\"blocked\"}"));

        assertThrows(MilkywayExposureBlockedException.class, () -> client.pay(payRequest(), "k"));
        assertEquals(1, server.hits("/payments/v1"));
    }

    @Test
    void payWithoutKeyIsNotRetriedOn5xx() {
        server.onPath("/payments/v1",
                c -> new StubServer.Response(503, "{\"error\":\"down\"}"));

        assertThrows(MilkywayServiceUnavailableException.class, () -> client.pay(payRequest()));
        assertEquals(1, server.hits("/payments/v1"), "pay without idempotency key must not be retried");
    }

    @Test
    void payWithKeyIsRetriedOn5xx() {
        server.onPathSequence("/payments/v1", List.of(
                new StubServer.Response(503, "{\"error\":\"down\"}"),
                new StubServer.Response(200, "{\"transaction_id\":5}")));

        long id = client.pay(payRequest(), "idem-key");

        assertEquals(5L, id);
        assertEquals(2, server.hits("/payments/v1"));
    }

    @Test
    void forceRefreshAndReplayOnceOn401() {
        // First attempt 401, replay (after force-refresh) 200.
        server.onPathSequence("/payments/v1/precheck", List.of(
                new StubServer.Response(401, "{\"error\":\"stale\"}"),
                new StubServer.Response(200, "{\"amount_credit\":1}")));

        PrecheckResult result = client.precheck(minimalPrecheck());

        assertNotNull(result);
        assertEquals(2, server.hits("/payments/v1/precheck"));
        assertEquals(1, tokens.forceRefreshes.get(), "exactly one forced refresh on 401");
        // Replay used the refreshed token.
        assertEquals("Bearer tok-refreshed", server.captured().get(1).header("Authorization"));
    }

    @Test
    void unparseableBodyRaisesApiException() {
        server.onPath("/payments/v1/precheck",
                c -> new StubServer.Response(200, "this is not json"));

        MilkywayApiException ex = assertThrows(MilkywayApiException.class,
                () -> client.precheck(minimalPrecheck()));
        assertTrue(ex.getMessage().contains("parse"));
    }

    private static PayRequest payRequest() {
        return new PayRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setSenderId("s1")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("10.00"))
                .setCurrencyCredit("USD");
    }

    private static PrecheckRequest minimalPrecheck() {
        return new PrecheckRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("10.00"))
                .setCurrencyCredit("USD")
                .setData(Collections.emptyMap());
    }
}
