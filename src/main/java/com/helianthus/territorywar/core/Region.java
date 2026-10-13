package com.helianthus.territorywar.core;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable named territory made of a fixed set of chunks, optionally owned by a team.
 * Ownership changes produce a new instance; persist them through {@code TerritorySavedData}.
 */
public final class Region {
    private final RegionId id;
    private final String name;
    private final Set<ChunkKey> chunks;
    private final int purchasePrice;
    private final boolean forSale;
    private final UUID ownerTeamId;

    public Region(RegionId id, String name, Set<ChunkKey> chunks, UUID ownerTeamId, int purchasePrice, boolean forSale) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.chunks = Set.copyOf(chunks);
        this.ownerTeamId = ownerTeamId;
        this.purchasePrice = purchasePrice;
        this.forSale = forSale;
    }

    public RegionId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Unmodifiable view of the region's chunks. */
    public Set<ChunkKey> getChunks() {
        return chunks;
    }

    /** Owning team, or {@code null} if unowned. */
    public UUID getOwnerTeamId() {
        return ownerTeamId;
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    public boolean isForSale() {
        return forSale;
    }

    public boolean containsChunk(ChunkKey chunk) {
        return chunks.contains(chunk);
    }

    public boolean isOwned() {
        return ownerTeamId != null;
    }

    /** Copy of this region owned by the given team. */
    public Region withOwner(UUID teamId) {
        return new Region(id, name, chunks, Objects.requireNonNull(teamId, "teamId"), purchasePrice, forSale);
    }

    /** Copy of this region with no owner. */
    public Region withoutOwner() {
        return new Region(id, name, chunks, null, purchasePrice, forSale);
    }
}
