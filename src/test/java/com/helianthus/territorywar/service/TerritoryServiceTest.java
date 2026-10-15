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
import java.util.UUID;
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

    @Test
    void completePurchaseAssignsOwnerAndClearsForSale() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        RegionId id = service.createRegion("spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), 100, true);
        data.setDirty(false);
        UUID team = UUID.randomUUID();

        assertEquals(PurchaseResult.SUCCESS, service.completePurchase(id, team));

        Region region = service.getRegion(id).orElseThrow();
        assertEquals(team, region.getOwnerTeamId());
        assertFalse(region.isForSale());
        assertTrue(data.isDirty());
    }

    @Test
    void completePurchaseRejectsRegionNotForSale() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        RegionId id = service.createRegion("spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), 100, false);
        data.setDirty(false);

        assertEquals(PurchaseResult.NOT_FOR_SALE, service.completePurchase(id, UUID.randomUUID()));

        assertFalse(service.getRegion(id).orElseThrow().isOwned());
        assertFalse(data.isDirty());
    }

    @Test
    void completePurchaseRejectsOwnedRegion() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        RegionId id = service.createRegion("spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), 100, true);
        UUID owner = UUID.randomUUID();
        data.assignOwner(id, owner);
        data.setDirty(false);

        assertEquals(PurchaseResult.ALREADY_OWNED, service.completePurchase(id, UUID.randomUUID()));

        Region region = service.getRegion(id).orElseThrow();
        assertEquals(owner, region.getOwnerTeamId());
        assertTrue(region.isForSale());
        assertFalse(data.isDirty());
    }

    @Test
    void completePurchaseOfUnknownRegionReturnsRegionNotFound() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);

        assertEquals(PurchaseResult.REGION_NOT_FOUND, service.completePurchase(RegionId.random(), UUID.randomUUID()));
        assertFalse(data.isDirty());
    }
}
