package com.helianthus.territorywar.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Chunk-to-region lookup. Each chunk maps to at most one region.
 */
public final class RegionIndex {
    private final Map<ChunkKey, RegionId> chunkToRegion = new HashMap<>();

    public Optional<RegionId> getRegion(ChunkKey chunk) {
        return Optional.ofNullable(chunkToRegion.get(Objects.requireNonNull(chunk, "chunk")));
    }

    /**
     * Indexes all chunks of the region. Rejects the whole region, leaving the index unchanged,
     * if any of its chunks is already mapped to a different region.
     */
    public void addRegion(Region region) {
        Objects.requireNonNull(region, "region");
        RegionId id = region.getId();
        for (ChunkKey chunk : region.getChunks()) {
            RegionId existing = chunkToRegion.get(chunk);
            if (existing != null && !existing.equals(id)) {
                throw new IllegalArgumentException("chunk " + chunk + " already belongs to region " + existing);
            }
        }
        for (ChunkKey chunk : region.getChunks()) {
            chunkToRegion.put(chunk, id);
        }
    }

    /** Removes every chunk mapped to the region. No-op if the region is not indexed. */
    public void removeRegion(RegionId regionId) {
        Objects.requireNonNull(regionId, "regionId");
        chunkToRegion.values().removeIf(regionId::equals);
    }

    public void clear() {
        chunkToRegion.clear();
    }
}
