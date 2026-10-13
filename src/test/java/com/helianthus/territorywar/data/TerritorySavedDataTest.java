package com.helianthus.territorywar.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class TerritorySavedDataTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void regionsRoundTripThroughNbt() {
        Region owned = new Region(RegionId.random(), "spawn",
                Set.of(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(-1, 3, OVERWORLD)), UUID.randomUUID(), 250, false);
        Region unowned = new Region(RegionId.random(), "outpost",
                Set.of(new ChunkKey(0, 0, NETHER)), null, 75, true);
        TerritorySavedData original = new TerritorySavedData();
        original.addRegion(owned);
        original.addRegion(unowned);

        TerritorySavedData loaded = TerritorySavedData.load(original.save(new CompoundTag()));

        assertEquals(2, loaded.getRegions().size());
        for (Region expected : new Region[] {owned, unowned}) {
            Region actual = loaded.getRegion(expected.getId()).orElseThrow();
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getChunks(), actual.getChunks());
            assertEquals(expected.getOwnerTeamId(), actual.getOwnerTeamId());
            assertEquals(expected.getPurchasePrice(), actual.getPurchasePrice());
            assertEquals(expected.isForSale(), actual.isForSale());
            for (ChunkKey chunk : expected.getChunks()) {
                assertEquals(Optional.of(expected.getId()), loaded.getIndex().getRegion(chunk));
            }
        }
        assertTrue(loaded.getIndex().getRegion(new ChunkKey(1, 1, OVERWORLD)).isEmpty());
        assertFalse(loaded.isDirty());
    }

    @Test
    void mutationsMarkDataDirty() {
        Region region = new Region(RegionId.random(), "spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), null, 100, true);
        TerritorySavedData data = new TerritorySavedData();

        data.addRegion(region);
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertTrue(data.assignOwner(region.getId(), UUID.randomUUID()));
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertTrue(data.clearOwner(region.getId()));
        assertNull(region.getOwnerTeamId());
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertTrue(data.removeRegion(region.getId()));
        assertTrue(data.isDirty());
        assertTrue(data.getRegion(region.getId()).isEmpty());
        assertTrue(data.getIndex().getRegion(new ChunkKey(0, 0, OVERWORLD)).isEmpty());
    }

    @Test
    void mutationsOnUnknownRegionLeaveDataClean() {
        TerritorySavedData data = new TerritorySavedData();
        RegionId missing = RegionId.random();

        assertFalse(data.removeRegion(missing));
        assertFalse(data.assignOwner(missing, UUID.randomUUID()));
        assertFalse(data.clearOwner(missing));
        assertFalse(data.isDirty());
    }
}
