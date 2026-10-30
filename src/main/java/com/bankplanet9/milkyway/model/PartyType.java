package com.bankplanet9.milkyway.model;

/** Wire values of {@link PartyInfo#getPartyType()}. */
public final class PartyType {

    /** A natural person. */
    public static final String NATURAL = "natural";

    /** A legal entity. Part of the contract, but not accepted by the API yet. */
    public static final String LEGAL = "legal";

    private PartyType() {
    }
}
