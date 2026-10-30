package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.exception.MilkywayApiException;
import com.bankplanet9.milkyway.exception.MilkywayPaymentRejectedException;
import com.bankplanet9.milkyway.exception.MilkywayRecipientBankException;
import com.bankplanet9.milkyway.exception.MilkywayServiceUnavailableException;
import com.bankplanet9.milkyway.exception.MilkywayValidationException;
import com.bankplanet9.milkyway.model.PayRequest;
import com.bankplanet9.milkyway.model.PostcheckResult;
import com.bankplanet9.milkyway.model.PrecheckRequest;
import com.bankplanet9.milkyway.model.StatusReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HTTP 422 / 502 / 503 mapping, the 400 field, and the postcheck reason codes. */
class TypedErrorsTest {

    private static final String REJECTED = "{\"error\":\"ErrPaymentRejected\"}";
    private static final String BANK_ERROR = "{\"error\":\"ErrRecipientBankError\"}";
    private static final String UNAVAILABLE =
            "{\"error\":\"ErrServiceUnavailable\",\"service_id\":\"wallet\",\"third_party_id\":\"recipient-bank\"}";

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
    void precheck422MapsToPaymentRejectedAndIsNotRetried() {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(422, REJECTED));

        MilkywayPaymentRejectedException ex = assertThrows(MilkywayPaymentRejectedException.class,
                () -> client.precheck(precheckRequest()));

        assertEquals(422, ex.getStatusCode());
        assertEquals("ErrPaymentRejected", ex.getErrorCode());
        assertEquals("ErrPaymentRejected", ex.getMessage());
        assertEquals(1, server.hits("/payments/v1/precheck"));
    }

    @Test
    void payWithKey422IsNotRetried() {
        server.onPath("/payments/v1", c -> new StubServer.Response(422, REJECTED));

        MilkywayPaymentRejectedException ex = assertThrows(MilkywayPaymentRejectedException.class,
                () -> client.pay(payRequest(), "idem-1"));

        assertEquals(StatusReason.PAYMENT_REJECTED, ex.getErrorCode());
        assertEquals(1, server.hits("/payments/v1"));
    }

    @Test
    void payWithKey502IsRecipientBankErrorAndServiceUnavailableAndIsRetried() {
        server.onPath("/payments/v1", c -> new StubServer.Response(502, BANK_ERROR));

        MilkywayServiceUnavailableException ex = assertThrows(MilkywayServiceUnavailableException.class,
                () -> client.pay(payRequest(), "idem-2"));

        MilkywayRecipientBankException bankEx = assertInstanceOf(MilkywayRecipientBankException.class, ex);
        assertEquals(502, bankEx.getStatusCode());
        assertEquals("ErrRecipientBankError", bankEx.getErrorCode());
        assertNull(bankEx.getServiceId());
        assertEquals(4, server.hits("/payments/v1"), "a 502 is transient: 1 attempt + maxRetries(3)");
    }

    @Test
    void payWithoutKey502IsNotRetried() {
        server.onPath("/payments/v1", c -> new StubServer.Response(502, BANK_ERROR));

        assertThrows(MilkywayRecipientBankException.class, () -> client.pay(payRequest()));
        assertEquals(1, server.hits("/payments/v1"), "pay without idempotency key must not be retried");
    }

    @Test
    void precheck502RecoversOnRetry() {
        server.onPathSequence("/payments/v1/precheck", List.of(
                new StubServer.Response(502, BANK_ERROR),
                new StubServer.Response(200, "{\"amount_credit\":1}")));

        client.precheck(precheckRequest());

        assertEquals(2, server.hits("/payments/v1/precheck"));
    }

    @Test
    void cancel503CarriesServiceAndThirdPartyIds() {
        server.onPath("/payments/v1/cancel", c -> new StubServer.Response(503, UNAVAILABLE));

        MilkywayServiceUnavailableException ex = assertThrows(MilkywayServiceUnavailableException.class,
                () -> client.cancel(42));

        assertEquals(503, ex.getStatusCode());
        assertEquals("ErrServiceUnavailable", ex.getErrorCode());
        assertEquals("wallet", ex.getServiceId());
        assertEquals("recipient-bank", ex.getThirdPartyId());
        assertEquals(4, server.hits("/payments/v1/cancel"));
    }

    @Test
    void cancel502MapsToRecipientBankError() {
        server.onPath("/payments/v1/cancel", c -> new StubServer.Response(502, BANK_ERROR));

        MilkywayRecipientBankException ex = assertThrows(MilkywayRecipientBankException.class,
                () -> client.cancel(42));

        assertEquals("ErrRecipientBankError", ex.getErrorCode());
    }

    @Test
    void cancel403IsPlainApiExceptionWithCode() {
        server.onPath("/payments/v1/cancel",
                c -> new StubServer.Response(403, "{\"error\":\"ErrPaymentCannotBeCanceled\"}"));

        MilkywayApiException ex = assertThrows(MilkywayApiException.class, () -> client.cancel(42));

        assertEquals(403, ex.getStatusCode());
        assertEquals(StatusReason.PAYMENT_CANNOT_BE_CANCELED, ex.getErrorCode());
        assertEquals(1, server.hits("/payments/v1/cancel"));
    }

    @Test
    void validationErrorExposesRecipientIdField() {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(400,
                "{\"error\":\"ErrRecipientInvalid\",\"field\":\"recipient_id\"}"));

        MilkywayValidationException ex = assertThrows(MilkywayValidationException.class,
                () -> client.precheck(precheckRequest()));

        assertEquals("recipient_id", ex.getField());
        assertEquals(StatusReason.RECIPIENT_INVALID, ex.getErrorCode());
    }

    @Test
    void validationErrorWithDetailsExposesCodePrefixAndField() {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(400,
                "{\"error\":\"ErrValidationFailed: currency_credit: XYZ is not a settlement currency\","
                        + "\"field\":\"currency_credit\"}"));

        MilkywayValidationException ex = assertThrows(MilkywayValidationException.class,
                () -> client.precheck(precheckRequest()));

        assertEquals("currency_credit", ex.getField());
        assertEquals("ErrValidationFailed", ex.getErrorCode());
        assertEquals("ErrValidationFailed: currency_credit: XYZ is not a settlement currency", ex.getMessage());
    }

    @Test
    void validationErrorWithoutCodeHasNullErrorCode() {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(400,
                "{\"error\":\"sender_info.date_of_birth: is required by the receiving bank for this service\","
                        + "\"field\":\"sender_info.date_of_birth\"}"));

        MilkywayValidationException ex = assertThrows(MilkywayValidationException.class,
                () -> client.precheck(precheckRequest()));

        assertEquals("sender_info.date_of_birth", ex.getField());
        assertNull(ex.getErrorCode());
    }

    @Test
    void errorCodeOfParsesOnlyLeadingErrIdentifiers() {
        assertEquals("ErrPaymentRejected", MilkywayPaymentsClient.errorCodeOf("ErrPaymentRejected"));
        assertEquals("ErrRecipientRateUnavailable",
                MilkywayPaymentsClient.errorCodeOf("ErrRecipientRateUnavailable: no rate for USD->TJS"));
        assertNull(MilkywayPaymentsClient.errorCodeOf("Errors happened"));
        assertNull(MilkywayPaymentsClient.errorCodeOf("Err"));
        assertNull(MilkywayPaymentsClient.errorCodeOf("amount must be positive"));
        assertNull(MilkywayPaymentsClient.errorCodeOf(null));
    }

    @Test
    void postcheckErrorCarriesEachReasonCode() {
        List<String> codes = List.of(StatusReason.PAYMENT_REJECTED, StatusReason.RECIPIENT_NOT_FOUND,
                StatusReason.RECIPIENT_INVALID, StatusReason.SERVICE_UNAVAILABLE,
                StatusReason.PAYMENT_CANNOT_BE_CANCELED);
        List<StubServer.Response> responses = new ArrayList<>();
        for (String code : codes) {
            responses.add(new StubServer.Response(200,
                    "{\"transaction_id\":7,\"status\":3,\"error\":\"" + code + "\"}"));
        }
        server.onPathSequence("/payments/v1/postcheck", responses);

        for (String code : codes) {
            PostcheckResult result = client.postcheck(7);
            assertEquals(code, result.getError());
        }
    }

    @Test
    void statusReasonConstantsMatchTheWireCodes() {
        assertEquals("ErrPaymentRejected", StatusReason.PAYMENT_REJECTED);
        assertEquals("ErrRecipientNotFound", StatusReason.RECIPIENT_NOT_FOUND);
        assertEquals("ErrRecipientInvalid", StatusReason.RECIPIENT_INVALID);
        assertEquals("ErrServiceUnavailable", StatusReason.SERVICE_UNAVAILABLE);
        assertEquals("ErrPaymentCannotBeCanceled", StatusReason.PAYMENT_CANNOT_BE_CANCELED);
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

    private static PrecheckRequest precheckRequest() {
        return new PrecheckRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("10.00"))
                .setCurrencyCredit("USD");
    }
}
