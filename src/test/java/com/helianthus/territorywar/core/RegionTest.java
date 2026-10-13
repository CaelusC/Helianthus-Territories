package com.helianthus.territorywar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegionTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private static Region unownedRegion(Set<ChunkKey> chunks) {
        return new Region(RegionId.random(), "spawn", chunks, null, 100, true);
    }

    @Test
    void newRegionWithoutOwnerIsNotOwned() {
        Region region = unownedRegion(Set.of());
        assertFalse(region.isOwned());
        assertNull(region.getOwnerTeamId());
    }

    @Test
    void assignOwnerMakesRegionOwned() {
        Region region = unownedRegion(Set.of());
        UUID team = UUID.randomUUID();
        region.assignOwner(team);
        assertTrue(region.isOwned());
        assertEquals(team, region.getOwnerTeamId());
    }

    @Test
    void assignOwnerReplacesExistingOwner() {
        Region region = new Region(RegionId.random(), "spawn", Set.of(), UUID.randomUUID(), 100, true);
        UUID newTeam = UUID.randomUUID();
        region.assignOwner(newTeam);
        assertEquals(newTeam, region.getOwnerTeamId());
    }

    @Test
    void clearOwnerMakesRegionUnowned() {
        Region region = unownedRegion(Set.of());
        region.assignOwner(UUID.randomUUID());
        region.clearOwner();
        assertFalse(region.isOwned());
        assertNull(region.getOwnerTeamId());
    }

    @Test
    void assignOwnerRejectsNull() {
        Region region = unownedRegion(Set.of());
        assertThrows(NullPointerException.class, () -> region.assignOwner(null));
    }

    @Test
    void containsChunkIsDimensionAware() {
        Region region = unownedRegion(Set.of(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(1, 0, OVERWORLD)));
        assertTrue(region.containsChunk(new ChunkKey(0, 0, OVERWORLD)));
        assertTrue(region.containsChunk(new ChunkKey(1, 0, OVERWORLD)));
        assertFalse(region.containsChunk(new ChunkKey(2, 0, OVERWORLD)));
        assertFalse(region.containsChunk(new ChunkKey(0, 0, NETHER)));
    }

    @Test
    void chunksAreDefensivelyCopiedAndUnmodifiable() {
        Set<ChunkKey> source = new HashSet<>();
        source.add(new ChunkKey(0, 0, OVERWORLD));
        Region region = unownedRegion(source);

        source.add(new ChunkKey(5, 5, OVERWORLD));
        assertFalse(region.containsChunk(new ChunkKey(5, 5, OVERWORLD)));
        assertThrows(UnsupportedOperationException.class, () -> region.getChunks().add(new ChunkKey(9, 9, OVERWORLD)));
    }

    @Test
    void exposesConstructorFields() {
        RegionId id = RegionId.random();
        Region region = new Region(id, "market", Set.of(), null, 250, false);
        assertEquals(id, region.getId());
        assertEquals("market", region.getName());
        assertEquals(250, region.getPurchasePrice());
        assertFalse(region.isForSale());
    }
}
