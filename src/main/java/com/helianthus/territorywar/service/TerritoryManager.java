package com.helianthus.territorywar.service;

import com.helianthus.territorywar.data.TerritoryDataService;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * Server-side access point for {@link TerritoryService}. Not cached: the saved data is already
 * cached by the overworld's data storage, and a static cache would outlive the server when an
 * integrated server is stopped and another world is opened.
 */
public final class TerritoryManager {
    private TerritoryManager() {
    }

    /** Returns a service bound to the server's {@link com.helianthus.territorywar.data.TerritorySavedData}. */
    public static TerritoryService get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return new TerritoryService(TerritoryDataService.get(server));
    }
}
