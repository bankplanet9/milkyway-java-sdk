package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single service a destination bank can receive payments through, as advertised
 * by the discovery catalog.
 *
 * <p>The service's JSON Schema is not included here — fetch it on demand with
 * {@link com.bankplanet9.milkyway.MilkywayPaymentsClient#serviceSchema(String, String)}.
 */
public final class ServiceCapability {

    @JsonProperty("service_id")
    private String serviceId;

    @JsonProperty("payout_method")
    private String payoutMethod;

    /** Recipient reference/label for this service, when the destination advertises one. */
    @JsonProperty("recipient")
    private String recipient;

    /** ISO-4217 currency codes this service can pay out in, when advertised. */
    @JsonProperty("payout_currencies")
    private List<String> payoutCurrencies;

    /**
     * Which {@link PartyInfo} fields the recipient bank requires and accepts for this
     * service, for {@code senderInfo} and {@code recipientInfo}. Null when the bank has
     * published none.
     */
    @JsonProperty("party_profile")
    private PartyProfile partyProfile;

    public String getServiceId() {
        return serviceId;
    }

    public String getPayoutMethod() {
        return payoutMethod;
    }

    public String getRecipient() {
        return recipient;
    }

    public List<String> getPayoutCurrencies() {
        return payoutCurrencies;
    }

    public PartyProfile getPartyProfile() {
        return partyProfile;
    }
}
