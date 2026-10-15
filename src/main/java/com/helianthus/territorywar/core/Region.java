package com.helianthus.territorywar.core;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable named territory made of a fixed set of chunks, optionally owned by a team.
 * Ownership changes produce a new instance; persist them through {@code TerritorySavedData}.
 */
public final class Region {
    /** Map colour of an unowned region whose admin did not pick one. */
    public static final int DEFAULT_COLOR = 0xFFD700;

    private final RegionId id;
    private final String name;
    private final Set<ChunkKey> chunks;
    private final int purchasePrice;
    private final boolean forSale;
    private final UUID ownerTeamId;
    private final int colorRgb;

    public Region(RegionId id, String name, Set<ChunkKey> chunks, UUID ownerTeamId, int purchasePrice, boolean forSale) {
        this(id, name, chunks, ownerTeamId, purchasePrice, forSale, DEFAULT_COLOR);
    }

    /** {@code colorRgb} is masked to its low 24 bits. */
    public Region(RegionId id, String name, Set<ChunkKey> chunks, UUID ownerTeamId, int purchasePrice, boolean forSale,
            int colorRgb) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.chunks = Set.copyOf(chunks);
        this.ownerTeamId = ownerTeamId;
        this.purchasePrice = purchasePrice;
        this.forSale = forSale;
        this.colorRgb = colorRgb & 0xFFFFFF;
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

    /** RGB map colour shown while the region is unowned. */
    public int getColorRgb() {
        return colorRgb;
    }

    public boolean containsChunk(ChunkKey chunk) {
        return chunks.contains(chunk);
    }

    public boolean isOwned() {
        return ownerTeamId != null;
    }

    /** Copy of this region owned by the given team. */
    public Region withOwner(UUID teamId) {
        return new Region(id, name, chunks, Objects.requireNonNull(teamId, "teamId"), purchasePrice, forSale, colorRgb);
    }

    /** Copy of this region with no owner. */
    public Region withoutOwner() {
        return new Region(id, name, chunks, null, purchasePrice, forSale, colorRgb);
    }

    /** Copy of this region with the given for-sale flag. */
    public Region withForSale(boolean forSale) {
        return new Region(id, name, chunks, ownerTeamId, purchasePrice, forSale, colorRgb);
    }
}
