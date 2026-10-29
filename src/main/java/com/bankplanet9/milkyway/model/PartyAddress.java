package com.bankplanet9.milkyway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A postal address. Text values are at most 256 characters; null and empty values
 * are omitted from the wire.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class PartyAddress {

    /** ISO 3166-1 alpha-2, upper case. Required when an address is sent. */
    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;

    @JsonProperty("street")
    private String street;

    @JsonProperty("building")
    private String building;

    @JsonProperty("postal_code")
    private String postalCode;

    /** An address held as free text, one line per element (at most 7 lines). */
    @JsonProperty("line")
    private List<String> line;

    public PartyAddress() {
    }

    public String getCountry() {
        return country;
    }

    public PartyAddress setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCity() {
        return city;
    }

    public PartyAddress setCity(String city) {
        this.city = city;
        return this;
    }

    public String getStreet() {
        return street;
    }

    public PartyAddress setStreet(String street) {
        this.street = street;
        return this;
    }

    public String getBuilding() {
        return building;
    }

    public PartyAddress setBuilding(String building) {
        this.building = building;
        return this;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public PartyAddress setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public List<String> getLine() {
        return line;
    }

    public PartyAddress setLine(List<String> line) {
        this.line = line;
        return this;
    }

    boolean isEmpty() {
        return isBlank(country) && isBlank(city) && isBlank(street) && isBlank(building)
                && isBlank(postalCode) && (line == null || line.isEmpty());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
