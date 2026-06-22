package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Quote for a prospective payment. The FX markup is already applied and is locked
 * onto the transaction if you proceed to {@code pay}.
 */
public final class PrecheckResult {

    @JsonProperty("third_party_id")
    private String thirdPartyId;

    @JsonProperty("service_id")
    private String serviceId;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("amount_credit")
    private BigDecimal amountCredit;

    @JsonProperty("currency_credit")
    private String currencyCredit;

    /** Amount the recipient bank is debited, in the debit currency. */
    @JsonProperty("amount_debit")
    private BigDecimal amountDebit;

    @JsonProperty("currency_debit")
    private String currencyDebit;

    /** Quoted exchange rate: {@code 1 CREDIT = rate DEBIT}. */
    @JsonProperty("rate")
    private BigDecimal rate;

    /** Commission charged, in the credit currency. */
    @JsonProperty("commission")
    private BigDecimal commission;

    public String getThirdPartyId() {
        return thirdPartyId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public String getCurrencyCredit() {
        return currencyCredit;
    }

    public BigDecimal getAmountDebit() {
        return amountDebit;
    }

    public String getCurrencyDebit() {
        return currencyDebit;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getCommission() {
        return commission;
    }
}
