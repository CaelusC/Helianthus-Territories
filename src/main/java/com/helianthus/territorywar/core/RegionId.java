package com.helianthus.territorywar.core;

import java.util.Objects;
import java.util.UUID;

public record RegionId(UUID value) {
    public RegionId {
        Objects.requireNonNull(value, "value");
    }

    public static RegionId random() {
        return new RegionId(UUID.randomUUID());
    }
}
