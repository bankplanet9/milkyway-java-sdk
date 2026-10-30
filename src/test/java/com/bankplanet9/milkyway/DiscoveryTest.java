package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.exception.MilkywayAuthException;
import com.bankplanet9.milkyway.exception.MilkywayNotFoundException;
import com.bankplanet9.milkyway.model.Destination;
import com.bankplanet9.milkyway.model.IdentityDocumentType;
import com.bankplanet9.milkyway.model.PartyProfile;
import com.bankplanet9.milkyway.model.PartyRequirements;
import com.bankplanet9.milkyway.model.ServiceCapability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryTest {

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
    void discoveryWithoutCountryParsesDestinationsAndOmitsQuery() {
        server.onPath("/payments/v1/discovery", c -> new StubServer.Response(200,
                "{\"destinations\":[{\"third_party_id\":\"bank-a\",\"partner_name\":\"Bank A\","
                        + "\"country\":\"TJ\",\"services\":[{\"service_id\":\"card-payout\","
                        + "\"payout_method\":\"card\",\"recipient\":\"r1\","
                        + "\"payout_currencies\":[\"USD\",\"TJS\"]}]}]}"));

        List<Destination> destinations = client.discovery();

        assertEquals(1, destinations.size());
        Destination d = destinations.get(0);
        assertEquals("bank-a", d.getThirdPartyId());
        assertEquals("Bank A", d.getPartnerName());
        assertEquals("TJ", d.getCountry());
        assertEquals(1, d.getServices().size());
        ServiceCapability svc = d.getServices().get(0);
        assertEquals("card-payout", svc.getServiceId());
        assertEquals("card", svc.getPayoutMethod());
        assertEquals("r1", svc.getRecipient());
        assertEquals(List.of("USD", "TJS"), svc.getPayoutCurrencies());

        StubServer.Captured req = server.captured().get(0);
        assertEquals("GET", req.method);
        assertNull(req.query, "no country filter must send no query string");
        assertEquals("Bearer tok", req.header("Authorization"));
    }

    @Test
    void discoveryDecodesPartyProfileAndToleratesItsAbsence() {
        server.onPath("/payments/v1/discovery", c -> new StubServer.Response(200,
                "{\"destinations\":[{\"third_party_id\":\"bank-a\",\"services\":["
                        + "{\"service_id\":\"card-payout\",\"party_profile\":{"
                        + "\"sender_info\":{\"required\":[\"name.first\",\"name.last\",\"date_of_birth\","
                        + "\"identity_document_type\",\"identity_document_number\"],"
                        + "\"accepted\":[\"name.full\"],\"identity_document_types\":[\"passport\"]},"
                        + "\"recipient_info\":{\"required\":[\"name.full\"]}}},"
                        + "{\"service_id\":\"wallet\"}]}]}"));

        List<ServiceCapability> services = client.discovery().get(0).getServices();

        PartyProfile profile = services.get(0).getPartyProfile();
        assertNotNull(profile);
        PartyRequirements sender = profile.getSenderInfo();
        assertEquals(List.of("name.first", "name.last", "date_of_birth", "identity_document_type",
                "identity_document_number"), sender.getRequired());
        assertEquals(List.of("name.full"), sender.getAccepted());
        assertEquals(List.of(IdentityDocumentType.PASSPORT), sender.getIdentityDocumentTypes());
        PartyRequirements recipient = profile.getRecipientInfo();
        assertEquals(List.of("name.full"), recipient.getRequired());
        assertNull(recipient.getAccepted());
        assertNull(recipient.getIdentityDocumentTypes());

        assertNull(services.get(1).getPartyProfile());
    }

    @Test
    void discoveryWithCountrySendsCountryQuery() {
        server.onPath("/payments/v1/discovery",
                c -> new StubServer.Response(200, "{\"destinations\":[]}"));

        List<Destination> destinations = client.discovery("TJ");

        assertTrue(destinations.isEmpty());
        assertEquals("country=TJ", server.captured().get(0).query);
    }

    @Test
    void discoveryTreatsBlankCountryAsUnfiltered() {
        server.onPath("/payments/v1/discovery",
                c -> new StubServer.Response(200, "{\"destinations\":[]}"));

        client.discovery("   ");

        assertNull(server.captured().get(0).query);
    }

    @Test
    void discoveryMissingDestinationsYieldsEmptyList() {
        server.onPath("/payments/v1/discovery", c -> new StubServer.Response(200, "{}"));

        assertTrue(client.discovery().isEmpty());
    }

    @Test
    void discovery401MapsToAuthException() {
        server.onPath("/payments/v1/discovery",
                c -> new StubServer.Response(401, "{\"error\":\"unauthorized\"}"));

        MilkywayAuthException ex = assertThrows(MilkywayAuthException.class, () -> client.discovery());
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void serviceSchemaParsesSchemaAndSendsEncodedPath() {
        server.onPath("/payments/v1/discovery", c -> new StubServer.Response(200,
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\","
                        + "\"required\":[\"pan\"],\"properties\":{\"pan\":{\"type\":\"string\"}}}"));

        Map<String, Object> schema = client.serviceSchema("bank a", "card-payout");

        assertEquals("object", schema.get("type"));
        assertEquals(List.of("pan"), schema.get("required"));
        assertTrue(schema.get("properties") instanceof Map);

        // The client had to percent-encode the space to build a legal URI; the server
        // reports the decoded path, so the space reappears here.
        StubServer.Captured req = server.captured().get(0);
        assertEquals("GET", req.method);
        assertEquals("/payments/v1/discovery/bank a/card-payout/schema", req.path);
    }

    @Test
    void serviceSchema404MapsToNotFound() {
        server.onPath("/payments/v1/discovery",
                c -> new StubServer.Response(404, "{\"error\":\"unknown service\"}"));

        MilkywayNotFoundException ex = assertThrows(MilkywayNotFoundException.class,
                () -> client.serviceSchema("bank-a", "nope"));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void serviceSchemaRequiresIds() {
        assertThrows(IllegalArgumentException.class, () -> client.serviceSchema("", "svc"));
        assertThrows(IllegalArgumentException.class, () -> client.serviceSchema("bank-a", "  "));
    }
}
