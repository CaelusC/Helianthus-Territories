package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.TerritorySavedData;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerritoryServiceTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void createRegionStoresAndIndexesRegion() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        ChunkKey a = new ChunkKey(0, 0, OVERWORLD);
        ChunkKey b = new ChunkKey(1, 0, OVERWORLD);

        RegionId id = service.createRegion("spawn", Set.of(a, b), 100, true);

        Region region = service.getRegion(id).orElseThrow();
        assertEquals("spawn", region.getName());
        assertEquals(Set.of(a, b), region.getChunks());
        assertFalse(region.isOwned());
        assertEquals(100, region.getPurchasePrice());
        assertTrue(region.isForSale());
        assertEquals(Optional.of(id), service.getRegionAt(a));
        assertEquals(Optional.of(id), service.getRegionAt(b));
        assertTrue(data.isDirty());
    }

    @Test
    void createRegionRejectsOverlapWithoutChangingState() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        ChunkKey shared = new ChunkKey(0, 0, OVERWORLD);
        ChunkKey free = new ChunkKey(5, 5, OVERWORLD);
        RegionId existing = service.createRegion("spawn", Set.of(shared), 100, true);
        data.setDirty(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.createRegion("outpost", Set.of(free, shared), 50, false));

        assertEquals(1, data.getRegions().size());
        assertEquals(Optional.of(existing), service.getRegionAt(shared));
        assertTrue(service.getRegionAt(free).isEmpty());
        assertFalse(data.isDirty());
    }

    @Test
    void deleteRegionRemovesRegionAndIndex() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        ChunkKey chunk = new ChunkKey(0, 0, OVERWORLD);
        RegionId id = service.createRegion("spawn", Set.of(chunk), 100, true);
        data.setDirty(false);

        assertTrue(service.deleteRegion(id));

        assertTrue(service.getRegion(id).isEmpty());
        assertTrue(service.getRegionAt(chunk).isEmpty());
        assertTrue(data.isDirty());
    }

    @Test
    void deleteUnknownRegionReturnsFalse() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);

        assertFalse(service.deleteRegion(RegionId.random()));
        assertFalse(data.isDirty());
    }
}
