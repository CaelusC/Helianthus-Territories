package com.helianthus.territorywar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegionIndexTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private static Region region(ChunkKey... chunks) {
        return new Region(RegionId.random(), "spawn", Set.of(chunks), null, 100, true);
    }

    @Test
    void emptyIndexReturnsNoRegion() {
        assertTrue(new RegionIndex().getRegion(new ChunkKey(0, 0, OVERWORLD)).isEmpty());
    }

    @Test
    void addedRegionIsFoundForEachOfItsChunks() {
        RegionIndex index = new RegionIndex();
        Region region = region(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(1, 0, OVERWORLD));
        index.addRegion(region);

        assertEquals(Optional.of(region.getId()), index.getRegion(new ChunkKey(0, 0, OVERWORLD)));
        assertEquals(Optional.of(region.getId()), index.getRegion(new ChunkKey(1, 0, OVERWORLD)));
        assertTrue(index.getRegion(new ChunkKey(2, 0, OVERWORLD)).isEmpty());
    }

    @Test
    void lookupIsDimensionAware() {
        RegionIndex index = new RegionIndex();
        Region overworld = region(new ChunkKey(0, 0, OVERWORLD));
        Region nether = region(new ChunkKey(0, 0, NETHER));
        index.addRegion(overworld);
        index.addRegion(nether);

        assertEquals(Optional.of(overworld.getId()), index.getRegion(new ChunkKey(0, 0, OVERWORLD)));
        assertEquals(Optional.of(nether.getId()), index.getRegion(new ChunkKey(0, 0, NETHER)));
    }

    @Test
    void overlappingRegionIsRejectedAndIndexIsUnchanged() {
        RegionIndex index = new RegionIndex();
        Region first = region(new ChunkKey(0, 0, OVERWORLD));
        index.addRegion(first);

        Region overlapping = region(new ChunkKey(5, 5, OVERWORLD), new ChunkKey(0, 0, OVERWORLD));
        assertThrows(IllegalArgumentException.class, () -> index.addRegion(overlapping));

        assertEquals(Optional.of(first.getId()), index.getRegion(new ChunkKey(0, 0, OVERWORLD)));
        assertTrue(index.getRegion(new ChunkKey(5, 5, OVERWORLD)).isEmpty());
    }

    @Test
    void removeRegionClearsOnlyItsChunks() {
        RegionIndex index = new RegionIndex();
        Region removed = region(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(1, 0, OVERWORLD));
        Region kept = region(new ChunkKey(2, 0, OVERWORLD));
        index.addRegion(removed);
        index.addRegion(kept);

        index.removeRegion(removed.getId());

        assertTrue(index.getRegion(new ChunkKey(0, 0, OVERWORLD)).isEmpty());
        assertTrue(index.getRegion(new ChunkKey(1, 0, OVERWORLD)).isEmpty());
        assertEquals(Optional.of(kept.getId()), index.getRegion(new ChunkKey(2, 0, OVERWORLD)));
    }

    @Test
    void removedRegionChunksCanBeClaimedByAnotherRegion() {
        RegionIndex index = new RegionIndex();
        Region first = region(new ChunkKey(0, 0, OVERWORLD));
        index.addRegion(first);
        index.removeRegion(first.getId());

        Region second = region(new ChunkKey(0, 0, OVERWORLD));
        index.addRegion(second);
        assertEquals(Optional.of(second.getId()), index.getRegion(new ChunkKey(0, 0, OVERWORLD)));
    }

    @Test
    void removeUnknownRegionIsNoOp() {
        RegionIndex index = new RegionIndex();
        Region region = region(new ChunkKey(0, 0, OVERWORLD));
        index.addRegion(region);

        index.removeRegion(RegionId.random());
        assertEquals(Optional.of(region.getId()), index.getRegion(new ChunkKey(0, 0, OVERWORLD)));
    }

    @Test
    void clearEmptiesTheIndex() {
        RegionIndex index = new RegionIndex();
        index.addRegion(region(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(0, 0, NETHER)));

        index.clear();

        assertTrue(index.getRegion(new ChunkKey(0, 0, OVERWORLD)).isEmpty());
        assertTrue(index.getRegion(new ChunkKey(0, 0, NETHER)).isEmpty());
    }
}
