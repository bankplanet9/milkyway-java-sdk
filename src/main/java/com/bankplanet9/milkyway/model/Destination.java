package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A destination bank you can pay, together with the services it can receive
 * payments through, as returned by the discovery catalog.
 */
public final class Destination {

    @JsonProperty("third_party_id")
    private String thirdPartyId;

    @JsonProperty("partner_name")
    private String partnerName;

    /** ISO 3166-1 alpha-2 country code of the destination bank, when advertised. */
    @JsonProperty("country")
    private String country;

    @JsonProperty("services")
    private List<ServiceCapability> services;

    public String getThirdPartyId() {
        return thirdPartyId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getCountry() {
        return country;
    }

    public List<ServiceCapability> getServices() {
        return services;
    }
}
