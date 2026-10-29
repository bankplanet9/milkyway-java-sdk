package com.bankplanet9.milkyway.model;

/**
 * Wire values of {@link PartyInfo#getIdentityDocumentType()} — a closed vocabulary,
 * one-to-one with the IVMS101 national identifier types.
 */
public final class IdentityDocumentType {

    /** Passport (IVMS101 {@code CCPT}). */
    public static final String PASSPORT = "passport";

    /** National identity card (IVMS101 {@code IDCD}). */
    public static final String NATIONAL_ID = "national_id";

    /** Driver's license (IVMS101 {@code DRLC}). */
    public static final String DRIVER_LICENSE = "driver_license";

    /** Residence permit (IVMS101 {@code ARNU}). */
    public static final String RESIDENCE_PERMIT = "residence_permit";

    /** Tax identification number (IVMS101 {@code TXID}). */
    public static final String TAX_ID = "tax_id";

    /** Social security number (IVMS101 {@code SOCS}). */
    public static final String SOCIAL_SECURITY = "social_security";

    /** Any other document (IVMS101 {@code MISC}). */
    public static final String OTHER = "other";

    private IdentityDocumentType() {
    }
}
