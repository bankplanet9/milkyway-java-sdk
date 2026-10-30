package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.internal.MilkywayJson;
import com.bankplanet9.milkyway.model.IdentityDocumentType;
import com.bankplanet9.milkyway.model.PartyAddress;
import com.bankplanet9.milkyway.model.PartyInfo;
import com.bankplanet9.milkyway.model.PartyName;
import com.bankplanet9.milkyway.model.PartyType;
import com.bankplanet9.milkyway.model.PayRequest;
import com.bankplanet9.milkyway.model.PrecheckRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PartyInfoTest {

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
    void precheckSerializesEveryPartyFieldUnderItsWireKey() throws Exception {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(200, "{}"));

        client.precheck(precheck().setSenderInfo(new PartyInfo()
                .setPartyType(PartyType.NATURAL)
                .setName(new PartyName().setFirst("Ivan").setLast("Petrov").setFull("Petrov Ivan Sergeevich"))
                .setDateOfBirth("1990-04-18")
                .setIdentityDocumentType(IdentityDocumentType.PASSPORT)
                .setIdentityDocumentNumber("AB1234567")
                .setIdentityDocumentCountry("TJ")
                .setCountryOfResidence("AE")
                .setAddress(new PartyAddress()
                        .setCountry("AE")
                        .setCity("Dubai")
                        .setStreet("Sheikh Zayed Rd")
                        .setBuilding("12")
                        .setPostalCode("00000")
                        .setLine(List.of("Sheikh Zayed Rd 12, Dubai")))));

        JsonNode body = body();
        assertEquals("{\"party_type\":\"natural\","
                        + "\"name\":{\"first\":\"Ivan\",\"last\":\"Petrov\",\"full\":\"Petrov Ivan Sergeevich\"},"
                        + "\"date_of_birth\":\"1990-04-18\","
                        + "\"identity_document_type\":\"passport\","
                        + "\"identity_document_number\":\"AB1234567\","
                        + "\"identity_document_country\":\"TJ\","
                        + "\"country_of_residence\":\"AE\","
                        + "\"address\":{\"country\":\"AE\",\"city\":\"Dubai\",\"street\":\"Sheikh Zayed Rd\","
                        + "\"building\":\"12\",\"postal_code\":\"00000\",\"line\":[\"Sheikh Zayed Rd 12, Dubai\"]}}",
                body.get("sender_info").toString());
        assertFalse(body.has("recipient_info"), "null recipient_info must be omitted: " + body);
    }

    @Test
    void payOmitsNullAndEmptyPartyFieldsAndEmptySubObjects() throws Exception {
        server.onPath("/payments/v1", c -> new StubServer.Response(200, "{\"transaction_id\":1}"));

        client.pay(new PayRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setSenderId("s1")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("10.00"))
                .setCurrencyCredit("USD")
                .setSenderInfo(new PartyInfo()
                        .setName(new PartyName().setFirst("Ivan").setLast("Petrov").setFull(""))
                        .setIdentityDocumentType(IdentityDocumentType.NATIONAL_ID)
                        .setIdentityDocumentNumber("A-1")
                        .setCountryOfResidence("")
                        .setAddress(new PartyAddress().setLine(Collections.emptyList())))
                .setRecipientInfo(new PartyInfo()
                        .setName(new PartyName())
                        .setAddress(new PartyAddress().setCountry("TJ").setCity("Dushanbe").setStreet(""))),
                "key-1");

        JsonNode body = body();
        assertEquals("{\"party_type\":\"natural\",\"name\":{\"first\":\"Ivan\",\"last\":\"Petrov\"},"
                        + "\"identity_document_type\":\"national_id\",\"identity_document_number\":\"A-1\"}",
                body.get("sender_info").toString());
        assertEquals("{\"party_type\":\"natural\",\"address\":{\"country\":\"TJ\",\"city\":\"Dushanbe\"}}",
                body.get("recipient_info").toString());
        assertFalse(body.has("data"), "null data must be omitted: " + body);
    }

    @Test
    void dataIsSentAlongsidePartyInfo() throws Exception {
        server.onPath("/payments/v1/precheck", c -> new StubServer.Response(200, "{}"));

        client.precheck(precheck()
                .setSenderInfo(new PartyInfo().setName(new PartyName().setFull("Petrov Ivan")))
                .setData(Map.of("test_outcome", "done")));

        JsonNode body = body();
        assertEquals("{\"party_type\":\"natural\",\"name\":{\"full\":\"Petrov Ivan\"}}",
                body.get("sender_info").toString());
        assertEquals("{\"test_outcome\":\"done\"}", body.get("data").toString());
    }

    @Test
    void identityDocumentTypeConstantsMatchTheWireVocabulary() {
        assertEquals(List.of("passport", "national_id", "driver_license", "residence_permit", "tax_id",
                        "social_security", "other"),
                List.of(IdentityDocumentType.PASSPORT, IdentityDocumentType.NATIONAL_ID,
                        IdentityDocumentType.DRIVER_LICENSE, IdentityDocumentType.RESIDENCE_PERMIT,
                        IdentityDocumentType.TAX_ID, IdentityDocumentType.SOCIAL_SECURITY,
                        IdentityDocumentType.OTHER));
        assertEquals("natural", PartyType.NATURAL);
        assertEquals("legal", PartyType.LEGAL);
    }

    private JsonNode body() throws Exception {
        return MilkywayJson.MAPPER.readTree(server.captured().get(0).body);
    }

    private static PrecheckRequest precheck() {
        return new PrecheckRequest()
                .setThirdPartyIdDebit("bank-a")
                .setServiceId("svc")
                .setRecipientId("r1")
                .setAmountCredit(new BigDecimal("10.00"))
                .setCurrencyCredit("USD");
    }
}
