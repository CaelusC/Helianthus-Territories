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
    void withOwnerReturnsOwnedCopyAndLeavesOriginalUnchanged() {
        Region region = unownedRegion(Set.of(new ChunkKey(0, 0, OVERWORLD)));
        UUID team = UUID.randomUUID();
        Region owned = region.withOwner(team);
        assertTrue(owned.isOwned());
        assertEquals(team, owned.getOwnerTeamId());
        assertFalse(region.isOwned());
        assertEquals(region.getId(), owned.getId());
        assertEquals(region.getName(), owned.getName());
        assertEquals(region.getChunks(), owned.getChunks());
        assertEquals(region.getPurchasePrice(), owned.getPurchasePrice());
        assertEquals(region.isForSale(), owned.isForSale());
    }

    @Test
    void colorDefaultsAndSurvivesCopies() {
        assertEquals(Region.DEFAULT_COLOR, unownedRegion(Set.of()).getColorRgb());
        Region region = new Region(RegionId.random(), "spawn", Set.of(), null, 100, true, 0xFF12AB34);
        assertEquals(0x12AB34, region.getColorRgb());
        assertEquals(0x12AB34, region.withOwner(UUID.randomUUID()).withoutOwner().withForSale(false).getColorRgb());
    }

    @Test
    void withOwnerReplacesExistingOwner() {
        Region region = new Region(RegionId.random(), "spawn", Set.of(), UUID.randomUUID(), 100, true);
        UUID newTeam = UUID.randomUUID();
        assertEquals(newTeam, region.withOwner(newTeam).getOwnerTeamId());
    }

    @Test
    void withoutOwnerReturnsUnownedCopy() {
        Region owned = unownedRegion(Set.of()).withOwner(UUID.randomUUID());
        Region cleared = owned.withoutOwner();
        assertFalse(cleared.isOwned());
        assertNull(cleared.getOwnerTeamId());
        assertTrue(owned.isOwned());
    }

    @Test
    void withOwnerRejectsNull() {
        Region region = unownedRegion(Set.of());
        assertThrows(NullPointerException.class, () -> region.withOwner(null));
    }

    @Test
    void withForSaleReturnsCopyAndKeepsOwner() {
        UUID team = UUID.randomUUID();
        Region region = unownedRegion(Set.of(new ChunkKey(0, 0, OVERWORLD))).withOwner(team);
        Region withdrawn = region.withForSale(false);
        assertFalse(withdrawn.isForSale());
        assertEquals(team, withdrawn.getOwnerTeamId());
        assertEquals(region.getId(), withdrawn.getId());
        assertEquals(region.getChunks(), withdrawn.getChunks());
        assertEquals(region.getPurchasePrice(), withdrawn.getPurchasePrice());
        assertTrue(region.isForSale());
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
