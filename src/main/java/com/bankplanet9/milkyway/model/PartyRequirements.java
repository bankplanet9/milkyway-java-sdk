package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Field requirements for one party object. Fields are named by their path within the
 * object — {@code name.first}, {@code name.last}, {@code name.full},
 * {@code date_of_birth}, {@code identity_document_type},
 * {@code identity_document_number}, {@code identity_document_country},
 * {@code country_of_residence}, {@code address.country}, {@code address.city},
 * {@code address.street}, {@code address.building}, {@code address.postal_code},
 * {@code address.line}. A field in neither list is not used by the bank.
 */
public final class PartyRequirements {

    /**
     * Fields a precheck or pay must carry. A missing one is refused with {@code 400}
     * naming it (e.g. {@code sender_info.date_of_birth}).
     */
    @JsonProperty("required")
    private List<String> required;

    /** Fields the bank uses when present. */
    @JsonProperty("accepted")
    private List<String> accepted;

    /**
     * The {@link IdentityDocumentType} values the bank takes. Null means any; another
     * kind is refused with {@code 400} naming {@code identity_document_type}.
     */
    @JsonProperty("identity_document_types")
    private List<String> identityDocumentTypes;

    public List<String> getRequired() {
        return required;
    }

    public List<String> getAccepted() {
        return accepted;
    }

    public List<String> getIdentityDocumentTypes() {
        return identityDocumentTypes;
    }
}
