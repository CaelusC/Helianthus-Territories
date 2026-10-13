package com.helianthus.territorywar.core;

import java.util.Objects;

/**
 * A chunk position qualified by its dimension id (e.g. {@code "minecraft:overworld"}).
 */
public record ChunkKey(int chunkX, int chunkZ, String dimension) {
    public ChunkKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    /**
     * Packs X into the low 32 bits and Z into the high 32 bits, matching vanilla's
     * {@code ChunkPos#asLong} layout. The dimension is not encoded.
     */
    public long toLong() {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static ChunkKey fromLong(long packed, String dimension) {
        return new ChunkKey((int) packed, (int) (packed >>> 32), dimension);
    }
}
