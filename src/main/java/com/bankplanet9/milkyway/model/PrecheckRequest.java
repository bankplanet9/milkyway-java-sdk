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
     * Service-specific payload, validated server-side against the service's JSON
     * Schema. Omitted from the wire when null.
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

    public Map<String, Object> getData() {
        return data;
    }

    public PrecheckRequest setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }
}
