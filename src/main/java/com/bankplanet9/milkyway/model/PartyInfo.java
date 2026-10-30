package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The canonical identity of a payment party — your customer ({@code sender_info}, the
 * originator) or the beneficiary ({@code recipient_info}). The shape is the same for
 * every recipient bank and service (IVMS101-anchored); the account, card or phone
 * stays in {@code senderId} / {@code recipientId}. Send what you have — which fields
 * a service requires is its {@link ServiceCapability#getPartyProfile() party profile}
 * from discovery.
 *
 * <p>Null and empty values, including an empty {@link PartyName} or
 * {@link PartyAddress}, are omitted from the wire; the server refuses any key outside
 * this model.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class PartyInfo {

    /**
     * Party kind, always sent. Defaults to {@link PartyType#NATURAL};
     * {@link PartyType#LEGAL} is not accepted by the API yet.
     */
    @JsonProperty("party_type")
    private String partyType = PartyType.NATURAL;

    /** The person's name. */
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = EmptyPartFilter.class)
    @JsonProperty("name")
    private PartyName name;

    /** Date of birth, ISO 8601 {@code YYYY-MM-DD} (e.g. {@code 1990-04-18}). */
    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    /**
     * Identity document kind — one of the {@link IdentityDocumentType} values.
     * Required with {@link #getIdentityDocumentNumber() identityDocumentNumber}.
     */
    @JsonProperty("identity_document_type")
    private String identityDocumentType;

    /** Document series and number together, as written in the document. */
    @JsonProperty("identity_document_number")
    private String identityDocumentNumber;

    /** Issuing country of the document, ISO 3166-1 alpha-2, upper case (e.g. {@code TJ}). */
    @JsonProperty("identity_document_country")
    private String identityDocumentCountry;

    /** Country of residence, ISO 3166-1 alpha-2, upper case (e.g. {@code AE}). */
    @JsonProperty("country_of_residence")
    private String countryOfResidence;

    /** Postal address. When sent, {@link PartyAddress#getCountry() country} is required. */
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = EmptyPartFilter.class)
    @JsonProperty("address")
    private PartyAddress address;

    public PartyInfo() {
    }

    public String getPartyType() {
        return partyType;
    }

    public PartyInfo setPartyType(String partyType) {
        this.partyType = partyType;
        return this;
    }

    public PartyName getName() {
        return name;
    }

    public PartyInfo setName(PartyName name) {
        this.name = name;
        return this;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public PartyInfo setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getIdentityDocumentType() {
        return identityDocumentType;
    }

    public PartyInfo setIdentityDocumentType(String identityDocumentType) {
        this.identityDocumentType = identityDocumentType;
        return this;
    }

    public String getIdentityDocumentNumber() {
        return identityDocumentNumber;
    }

    public PartyInfo setIdentityDocumentNumber(String identityDocumentNumber) {
        this.identityDocumentNumber = identityDocumentNumber;
        return this;
    }

    public String getIdentityDocumentCountry() {
        return identityDocumentCountry;
    }

    public PartyInfo setIdentityDocumentCountry(String identityDocumentCountry) {
        this.identityDocumentCountry = identityDocumentCountry;
        return this;
    }

    public String getCountryOfResidence() {
        return countryOfResidence;
    }

    public PartyInfo setCountryOfResidence(String countryOfResidence) {
        this.countryOfResidence = countryOfResidence;
        return this;
    }

    public PartyAddress getAddress() {
        return address;
    }

    public PartyInfo setAddress(PartyAddress address) {
        this.address = address;
        return this;
    }

    /**
     * Jackson value filter that drops an empty {@link PartyName} / {@link PartyAddress}
     * so the wire never carries {@code "name":{}} or {@code "address":{}}.
     */
    static final class EmptyPartFilter {

        @Override
        public boolean equals(Object value) {
            if (value == null) {
                return true;
            }
            if (value instanceof PartyName) {
                return ((PartyName) value).isEmpty();
            }
            if (value instanceof PartyAddress) {
                return ((PartyAddress) value).isEmpty();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
