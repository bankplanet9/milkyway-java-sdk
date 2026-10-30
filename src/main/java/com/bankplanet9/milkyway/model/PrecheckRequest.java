package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request for a payment quote (rate + commission) without creating a payment.
 * The credit party (your institution) is resolved from the access token's
 * {@code azp} claim and must not be sent here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PrecheckRequest {

    @JsonProperty("third_party_id_debit")
    private String thirdPartyIdDebit;

    @JsonProperty("service_id")
    private String serviceId;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("amount_credit")
    private BigDecimal amountCredit;

    @JsonProperty("currency_credit")
    private String currencyCredit;

    /**
     * Your customer, the originator ({@code sender_info}). Which fields are required is
     * the service's {@link ServiceCapability#getPartyProfile() party profile} from
     * discovery. Omitted from the wire when null.
     */
    @JsonProperty("sender_info")
    private PartyInfo senderInfo;

    /**
     * The beneficiary ({@code recipient_info}). Which fields are required is the
     * service's {@link ServiceCapability#getPartyProfile() party profile} from
     * discovery. Omitted from the wire when null.
     */
    @JsonProperty("recipient_info")
    private PartyInfo recipientInfo;

    /**
     * A service's non-identity extras, validated by the recipient bank during precheck.
     * Never the payer's or beneficiary's identity — that goes in
     * {@link #getSenderInfo() senderInfo} / {@link #getRecipientInfo() recipientInfo}.
     * Most services take no fields; when one does, its JSON Schema is returned by
     * {@link com.bankplanet9.milkyway.MilkywayPaymentsClient#serviceSchema(String, String)}.
     * Omitted from the wire when null.
     */
    @JsonProperty("data")
    private Map<String, Object> data;

    public PrecheckRequest() {
    }

    public String getThirdPartyIdDebit() {
        return thirdPartyIdDebit;
    }

    public PrecheckRequest setThirdPartyIdDebit(String thirdPartyIdDebit) {
        this.thirdPartyIdDebit = thirdPartyIdDebit;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public PrecheckRequest setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public PrecheckRequest setRecipientId(String recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public PrecheckRequest setAmountCredit(BigDecimal amountCredit) {
        this.amountCredit = amountCredit;
        return this;
    }

    public String getCurrencyCredit() {
        return currencyCredit;
    }

    public PrecheckRequest setCurrencyCredit(String currencyCredit) {
        this.currencyCredit = currencyCredit;
        return this;
    }

    public PartyInfo getSenderInfo() {
        return senderInfo;
    }

    public PrecheckRequest setSenderInfo(PartyInfo senderInfo) {
        this.senderInfo = senderInfo;
        return this;
    }

    public PartyInfo getRecipientInfo() {
        return recipientInfo;
    }

    public PrecheckRequest setRecipientInfo(PartyInfo recipientInfo) {
        this.recipientInfo = recipientInfo;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public PrecheckRequest setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }
}
