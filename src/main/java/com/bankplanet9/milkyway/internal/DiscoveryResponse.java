package com.bankplanet9.milkyway.internal;

import com.bankplanet9.milkyway.model.Destination;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Wire envelope of a discovery listing: {@code {"destinations": [...]}}. */
public final class DiscoveryResponse {

    @JsonProperty("destinations")
    private List<Destination> destinations;

    public List<Destination> getDestinations() {
        return destinations;
    }
}
