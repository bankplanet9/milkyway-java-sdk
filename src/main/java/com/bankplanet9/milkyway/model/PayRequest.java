package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request to initiate a cross-bank payment. The credit party (your institution)
 * is resolved from the access token's {@code azp} claim — do not send
 * {@code third_party_id_credit}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PayRequest {

    @JsonProperty("third_party_id_debit")
    private String thirdPartyIdDebit;

    @JsonProperty("service_id")
    private String serviceId;

    @JsonProperty("sender_id")
    private String senderId;

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

    public PayRequest() {
    }

    public String getThirdPartyIdDebit() {
        return thirdPartyIdDebit;
    }

    public PayRequest setThirdPartyIdDebit(String thirdPartyIdDebit) {
        this.thirdPartyIdDebit = thirdPartyIdDebit;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public PayRequest setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public PayRequest setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public PayRequest setRecipientId(String recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public PayRequest setAmountCredit(BigDecimal amountCredit) {
        this.amountCredit = amountCredit;
        return this;
    }

    public String getCurrencyCredit() {
        return currencyCredit;
    }

    public PayRequest setCurrencyCredit(String currencyCredit) {
        this.currencyCredit = currencyCredit;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public PayRequest setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }
}
