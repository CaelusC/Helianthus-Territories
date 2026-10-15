package com.helianthus.territorywar.client;

import com.helianthus.territorywar.core.Region;
import java.util.List;

/** Regions last synced from the server. Only touched on the client thread. */
public final class ClientRegionCache {
    private static List<Region> regions = List.of();

    private ClientRegionCache() {
    }

    public static List<Region> getRegions() {
        return regions;
    }

    static void setRegions(List<Region> synced) {
        regions = List.copyOf(synced);
    }

    static void clear() {
        regions = List.of();
    }
}
