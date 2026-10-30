package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A natural person's name. Each part is optional and at most 256 characters; null
 * and empty parts are omitted from the wire.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class PartyName {

    /** Given name. */
    @JsonProperty("first")
    private String first;

    /** Family name. */
    @JsonProperty("last")
    private String last;

    /**
     * The name as written in the document, family name first, with a patronymic if
     * any (e.g. {@code Petrov Ivan Sergeevich}).
     */
    @JsonProperty("full")
    private String full;

    public PartyName() {
    }

    public String getFirst() {
        return first;
    }

    public PartyName setFirst(String first) {
        this.first = first;
        return this;
    }

    public String getLast() {
        return last;
    }

    public PartyName setLast(String last) {
        this.last = last;
        return this;
    }

    public String getFull() {
        return full;
    }

    public PartyName setFull(String full) {
        this.full = full;
        return this;
    }

    boolean isEmpty() {
        return isBlank(first) && isBlank(last) && isBlank(full);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
