package com.helianthus.territorywar.data;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.core.RegionIndex;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

/**
 * Persists all regions and rebuilds the {@link RegionIndex} on load.
 */
public final class TerritorySavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Bump when the NBT layout changes and add a migration path in {@link #load}. */
    public static final int DATA_VERSION = 1;

    private static final String TAG_DATA_VERSION = "dataVersion";
    private static final String TAG_REGION_COUNT = "regionCount";
    private static final String TAG_REGIONS = "regions";
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_OWNER_TEAM_ID = "ownerTeamId";
    private static final String TAG_PRICE = "price";
    private static final String TAG_FOR_SALE = "forSale";
    private static final String TAG_COLOR = "color";
    private static final String TAG_CHUNKS = "chunks";
    private static final String TAG_CHUNK_X = "x";
    private static final String TAG_CHUNK_Z = "z";
    private static final String TAG_DIMENSION = "dimension";

    private final Map<RegionId, Region> regions = new LinkedHashMap<>();
    private final RegionIndex index = new RegionIndex();

    public Optional<Region> getRegion(RegionId id) {
        return Optional.ofNullable(regions.get(Objects.requireNonNull(id, "id")));
    }

    /** Unmodifiable view of all stored regions. */
    public Collection<Region> getRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public RegionIndex getIndex() {
        return index;
    }

    /**
     * Stores the region and indexes its chunks. Rejects the region, leaving storage unchanged,
     * if its id is already stored or any chunk belongs to another region.
     */
    public void addRegion(Region region) {
        Objects.requireNonNull(region, "region");
        if (regions.containsKey(region.getId())) {
            throw new IllegalArgumentException("region " + region.getId() + " already stored");
        }
        index.addRegion(region);
        regions.put(region.getId(), region);
        setDirty();
    }

    /** Removes the region and its chunk mappings. Returns {@code false} if it was not stored. */
    public boolean removeRegion(RegionId id) {
        Objects.requireNonNull(id, "id");
        if (regions.remove(id) == null) {
            return false;
        }
        index.removeRegion(id);
        setDirty();
        return true;
    }

    /**
     * Replaces the region's chunks and re-indexes them. Rejects the change, leaving storage unchanged,
     * if any chunk belongs to another region. Returns {@code false} if the region is not stored.
     */
    public boolean setChunks(RegionId id, Set<ChunkKey> chunks) {
        Region region = regions.get(Objects.requireNonNull(id, "id"));
        if (region == null) {
            return false;
        }
        Region updated = region.withChunks(chunks);
        for (ChunkKey chunk : updated.getChunks()) {
            Optional<RegionId> existing = index.getRegion(chunk);
            if (existing.isPresent() && !existing.get().equals(id)) {
                throw new IllegalArgumentException("chunk " + chunk + " already belongs to region " + existing.get());
            }
        }
        index.removeRegion(id);
        index.addRegion(updated);
        regions.put(id, updated);
        setDirty();
        return true;
    }

    /** Assigns the owning team. Returns {@code false} if the region is not stored. */
    public boolean assignOwner(RegionId id, UUID teamId) {
        Region region = regions.get(Objects.requireNonNull(id, "id"));
        if (region == null) {
            return false;
        }
        regions.put(id, region.withOwner(teamId));
        setDirty();
        return true;
    }

    /** Clears the owning team. Returns {@code false} if the region is not stored. */
    public boolean clearOwner(RegionId id) {
        Region region = regions.get(Objects.requireNonNull(id, "id"));
        if (region == null) {
            return false;
        }
        regions.put(id, region.withoutOwner());
        setDirty();
        return true;
    }

    /** Sets whether the region is for sale. Returns {@code false} if the region is not stored. */
    public boolean setForSale(RegionId id, boolean forSale) {
        Region region = regions.get(Objects.requireNonNull(id, "id"));
        if (region == null) {
            return false;
        }
        regions.put(id, region.withForSale(forSale));
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
        tag.putInt(TAG_REGION_COUNT, regions.size());
        ListTag regionList = new ListTag();
        for (Region region : regions.values()) {
            regionList.add(writeRegion(region));
        }
        tag.put(TAG_REGIONS, regionList);
        return tag;
    }

    /**
     * Reads data written by {@link #save}. Vanilla discards the data if this throws,
     * so problems are logged and loading continues with whatever can be read.
     */
    public static TerritorySavedData load(CompoundTag tag) {
        int version = tag.getInt(TAG_DATA_VERSION);
        if (version != DATA_VERSION) {
            LOGGER.error("Territory data version {} does not match supported version {}; loading known fields only",
                    version, DATA_VERSION);
        }

        TerritorySavedData data = new TerritorySavedData();
        ListTag regionList = tag.getList(TAG_REGIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < regionList.size(); i++) {
            Region region = readRegion(regionList.getCompound(i));
            try {
                data.index.addRegion(region);
                data.regions.put(region.getId(), region);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Skipping region {} ({}): {}", region.getId().value(), region.getName(), e.getMessage());
            }
        }

        int expectedCount = tag.getInt(TAG_REGION_COUNT);
        if (expectedCount != data.regions.size()) {
            LOGGER.warn("Territory data declared {} regions but {} were loaded", expectedCount, data.regions.size());
        }
        return data;
    }

    private static CompoundTag writeRegion(Region region) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_ID, region.getId().value());
        tag.putString(TAG_NAME, region.getName());
        if (region.getOwnerTeamId() != null) {
            tag.putUUID(TAG_OWNER_TEAM_ID, region.getOwnerTeamId());
        }
        tag.putInt(TAG_PRICE, region.getPurchasePrice());
        tag.putBoolean(TAG_FOR_SALE, region.isForSale());
        tag.putInt(TAG_COLOR, region.getColorRgb());
        ListTag chunkList = new ListTag();
        for (ChunkKey chunk : region.getChunks()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putInt(TAG_CHUNK_X, chunk.chunkX());
            chunkTag.putInt(TAG_CHUNK_Z, chunk.chunkZ());
            chunkTag.putString(TAG_DIMENSION, chunk.dimension());
            chunkList.add(chunkTag);
        }
        tag.put(TAG_CHUNKS, chunkList);
        return tag;
    }

    private static Region readRegion(CompoundTag tag) {
        RegionId id = new RegionId(tag.getUUID(TAG_ID));
        UUID owner = tag.hasUUID(TAG_OWNER_TEAM_ID) ? tag.getUUID(TAG_OWNER_TEAM_ID) : null;
        ListTag chunkList = tag.getList(TAG_CHUNKS, Tag.TAG_COMPOUND);
        Set<ChunkKey> chunks = new HashSet<>();
        for (int i = 0; i < chunkList.size(); i++) {
            CompoundTag chunkTag = chunkList.getCompound(i);
            chunks.add(new ChunkKey(chunkTag.getInt(TAG_CHUNK_X), chunkTag.getInt(TAG_CHUNK_Z),
                    chunkTag.getString(TAG_DIMENSION)));
        }
        int color = tag.contains(TAG_COLOR, Tag.TAG_INT) ? tag.getInt(TAG_COLOR) : Region.DEFAULT_COLOR;
        return new Region(id, tag.getString(TAG_NAME), chunks, owner, tag.getInt(TAG_PRICE),
                tag.getBoolean(TAG_FOR_SALE), color);
    }
}
