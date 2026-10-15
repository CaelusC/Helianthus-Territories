package com.helianthus.territorywar.service;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.TerritorySavedData;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Single entrypoint for creating, deleting and selling regions. Keeps {@link TerritorySavedData} and its
 * region index in sync and logs one audit line per state change.
 */
public final class TerritoryService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final TerritorySavedData data;

    public TerritoryService(TerritorySavedData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    /**
     * Creates an unowned region and stores it. Fails with {@link IllegalArgumentException},
     * leaving state unchanged, if any chunk already belongs to another region.
     */
    public RegionId createRegion(String name, Set<ChunkKey> chunks, int purchasePrice, boolean forSale) {
        return createRegion(name, chunks, purchasePrice, forSale, Region.DEFAULT_COLOR);
    }

    /** Like {@link #createRegion(String, Set, int, boolean)}, shown on the map in {@code colorRgb} while unowned. */
    public RegionId createRegion(String name, Set<ChunkKey> chunks, int purchasePrice, boolean forSale, int colorRgb) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(chunks, "chunks");
        for (ChunkKey chunk : chunks) {
            Optional<RegionId> existing = data.getIndex().getRegion(chunk);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("chunk " + chunk + " already belongs to region " + existing.get());
            }
        }
        Region region = new Region(RegionId.random(), name, chunks, null, purchasePrice, forSale, colorRgb);
        data.addRegion(region);
        LOGGER.info("Region created: id={} name={} chunks={} price={} forSale={}",
                region.getId().value(), name, region.getChunks().size(), purchasePrice, forSale);
        return region.getId();
    }

    /** Removes the region and its chunk mappings. Returns {@code false} if it was not stored. */
    public boolean deleteRegion(RegionId regionId) {
        Objects.requireNonNull(regionId, "regionId");
        Optional<Region> region = data.getRegion(regionId);
        if (region.isEmpty()) {
            return false;
        }
        data.removeRegion(regionId);
        LOGGER.info("Region deleted: id={} name={} chunks={}",
                regionId.value(), region.get().getName(), region.get().getChunks().size());
        return true;
    }

    /**
     * Adds and removes chunks of a stored region; removal wins if a chunk is in both sets. Fails with
     * {@link IllegalArgumentException}, leaving state unchanged, if an added chunk belongs to another
     * region or no chunks would be left. Returns {@code false} if the region is not stored.
     */
    public boolean editRegionChunks(RegionId regionId, Set<ChunkKey> added, Set<ChunkKey> removed) {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(added, "added");
        Objects.requireNonNull(removed, "removed");
        Optional<Region> region = data.getRegion(regionId);
        if (region.isEmpty()) {
            return false;
        }
        Set<ChunkKey> chunks = new HashSet<>(region.get().getChunks());
        chunks.addAll(added);
        chunks.removeAll(removed);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("region " + regionId + " would have no chunks left");
        }
        data.setChunks(regionId, chunks);
        LOGGER.info("Region chunks edited: id={} name={} chunks={} -> {}",
                regionId.value(), region.get().getName(), region.get().getChunks().size(), chunks.size());
        return true;
    }

    /**
     * Gives an unowned, for-sale region to the team and takes it off the market. Returns why it
     * failed, leaving state unchanged, if the region is unknown, owned or not for sale.
     */
    public PurchaseResult completePurchase(RegionId regionId, UUID teamId) {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(teamId, "teamId");
        Optional<Region> region = data.getRegion(regionId);
        PurchaseResult availability = PurchaseResult.availability(region);
        if (availability != PurchaseResult.SUCCESS) {
            return availability;
        }
        data.assignOwner(regionId, teamId);
        data.setForSale(regionId, false);
        LOGGER.info("Region sold: id={} name={} team={} chunks={} price={}",
                regionId.value(), region.get().getName(), teamId, region.get().getChunks().size(),
                region.get().getPurchasePrice());
        return PurchaseResult.SUCCESS;
    }

    public Optional<Region> getRegion(RegionId id) {
        return data.getRegion(id);
    }

    public Optional<RegionId> getRegionAt(ChunkKey chunk) {
        return data.getIndex().getRegion(chunk);
    }
}
