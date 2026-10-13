package com.helianthus.territorywar.data;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * Fetches the server-wide {@link TerritorySavedData}, stored in the overworld's data storage
 * so it is loaded on first access and saved with the level whenever it is dirty.
 */
public final class TerritoryDataService {
    /** Saved as {@code data/helianthus_territories.dat} in the world folder. */
    public static final String DATA_NAME = "helianthus_territories";

    private TerritoryDataService() {
    }

    public static TerritorySavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage()
                .computeIfAbsent(TerritorySavedData::load, TerritorySavedData::new, DATA_NAME);
    }
}
