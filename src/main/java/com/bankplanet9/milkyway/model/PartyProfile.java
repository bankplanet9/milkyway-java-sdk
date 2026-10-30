package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Which {@link PartyInfo} fields a service's recipient bank requires and accepts —
 * the same profile the bank enforces at precheck and pay. A party object the profile
 * does not list needs no fields.
 */
public final class PartyProfile {

    /** Requirements for {@code senderInfo} (your customer), when the bank lists any. */
    @JsonProperty("sender_info")
    private PartyRequirements senderInfo;

    /** Requirements for {@code recipientInfo} (the beneficiary), when the bank lists any. */
    @JsonProperty("recipient_info")
    private PartyRequirements recipientInfo;

    public PartyRequirements getSenderInfo() {
        return senderInfo;
    }

    public PartyRequirements getRecipientInfo() {
        return recipientInfo;
    }
}
