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
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class TerritorySavedDataTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void regionsRoundTripThroughNbt() {
        Region owned = new Region(RegionId.random(), "spawn",
                Set.of(new ChunkKey(0, 0, OVERWORLD), new ChunkKey(-1, 3, OVERWORLD)), UUID.randomUUID(), 250, false);
        Region unowned = new Region(RegionId.random(), "outpost",
                Set.of(new ChunkKey(0, 0, NETHER)), null, 75, true, 0x40E0D0);
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
            assertEquals(expected.getColorRgb(), actual.getColorRgb());
            for (ChunkKey chunk : expected.getChunks()) {
                assertEquals(Optional.of(expected.getId()), loaded.getIndex().getRegion(chunk));
            }
        }
        assertTrue(loaded.getIndex().getRegion(new ChunkKey(1, 1, OVERWORLD)).isEmpty());
        assertFalse(loaded.isDirty());
    }

    @Test
    void regionSavedWithoutColorLoadsDefaultColor() {
        Region region = new Region(RegionId.random(), "spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), null, 100, true,
                0x40E0D0);
        TerritorySavedData original = new TerritorySavedData();
        original.addRegion(region);
        CompoundTag tag = original.save(new CompoundTag());
        tag.getList("regions", Tag.TAG_COMPOUND).getCompound(0).remove("color");

        TerritorySavedData loaded = TerritorySavedData.load(tag);

        assertEquals(Region.DEFAULT_COLOR, loaded.getRegion(region.getId()).orElseThrow().getColorRgb());
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
        assertNull(data.getRegion(region.getId()).orElseThrow().getOwnerTeamId());
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertTrue(data.removeRegion(region.getId()));
        assertTrue(data.isDirty());
        assertTrue(data.getRegion(region.getId()).isEmpty());
        assertTrue(data.getIndex().getRegion(new ChunkKey(0, 0, OVERWORLD)).isEmpty());
    }

    @Test
    void ownershipChangesUpdateStoredRegionAndKeepIndex() {
        ChunkKey chunk = new ChunkKey(0, 0, OVERWORLD);
        Region region = new Region(RegionId.random(), "spawn", Set.of(chunk), null, 100, true);
        TerritorySavedData data = new TerritorySavedData();
        data.addRegion(region);
        data.setDirty(false);

        UUID team = UUID.randomUUID();
        assertTrue(data.assignOwner(region.getId(), team));
        assertTrue(data.isDirty());
        Region stored = data.getRegion(region.getId()).orElseThrow();
        assertEquals(team, stored.getOwnerTeamId());
        assertEquals(region.getChunks(), stored.getChunks());
        assertEquals(1, data.getRegions().size());
        assertEquals(Optional.of(region.getId()), data.getIndex().getRegion(chunk));
        assertNull(region.getOwnerTeamId());

        data.setDirty(false);
        assertTrue(data.clearOwner(region.getId()));
        assertTrue(data.isDirty());
        assertFalse(data.getRegion(region.getId()).orElseThrow().isOwned());
        assertEquals(1, data.getRegions().size());
        assertEquals(Optional.of(region.getId()), data.getIndex().getRegion(chunk));
    }

    @Test
    void setForSaleUpdatesStoredRegionAndKeepsOwner() {
        UUID team = UUID.randomUUID();
        Region region = new Region(RegionId.random(), "spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), team, 100, true);
        TerritorySavedData data = new TerritorySavedData();
        data.addRegion(region);
        data.setDirty(false);

        assertTrue(data.setForSale(region.getId(), false));

        Region stored = data.getRegion(region.getId()).orElseThrow();
        assertFalse(stored.isForSale());
        assertEquals(team, stored.getOwnerTeamId());
        assertTrue(data.isDirty());
    }

    @Test
    void mutationsOnUnknownRegionLeaveDataClean() {
        TerritorySavedData data = new TerritorySavedData();
        RegionId missing = RegionId.random();

        assertFalse(data.removeRegion(missing));
        assertFalse(data.assignOwner(missing, UUID.randomUUID()));
        assertFalse(data.clearOwner(missing));
        assertFalse(data.setForSale(missing, false));
        assertFalse(data.isDirty());
    }

    @Test
    void raidCooldownsRoundTripThroughNbt() {
        UUID victim = UUID.randomUUID();
        UUID raider = UUID.randomUUID();
        UUID both = UUID.randomUUID();
        TerritorySavedData original = new TerritorySavedData();
        original.getRaidCooldowns().setVictimCooldownUntil(victim, 1_700_000_000_000L);
        original.getRaidCooldowns().setRaiderCooldownUntil(raider, 1_800_000_000_000L);
        original.getRaidCooldowns().setVictimCooldownUntil(both, 123L);
        original.getRaidCooldowns().setRaiderCooldownUntil(both, 456L);

        TerritorySavedData loaded = TerritorySavedData.load(original.save(new CompoundTag()));

        RaidCooldownTracker cooldowns = loaded.getRaidCooldowns();
        assertEquals(1_700_000_000_000L, cooldowns.getVictimCooldownUntil(victim));
        assertEquals(0, cooldowns.getRaiderCooldownUntil(victim));
        assertEquals(0, cooldowns.getVictimCooldownUntil(raider));
        assertEquals(1_800_000_000_000L, cooldowns.getRaiderCooldownUntil(raider));
        assertEquals(123L, cooldowns.getVictimCooldownUntil(both));
        assertEquals(456L, cooldowns.getRaiderCooldownUntil(both));
        assertEquals(0, cooldowns.getVictimCooldownUntil(UUID.randomUUID()));
        assertFalse(loaded.isDirty());
    }

    @Test
    void dataSavedWithoutRaidCooldownsLoadsNone() {
        UUID team = UUID.randomUUID();
        TerritorySavedData original = new TerritorySavedData();
        original.getRaidCooldowns().setVictimCooldownUntil(team, 1_000L);
        CompoundTag tag = original.save(new CompoundTag());
        tag.remove("raidCooldowns");

        TerritorySavedData loaded = TerritorySavedData.load(tag);

        assertEquals(0, loaded.getRaidCooldowns().getVictimCooldownUntil(team));
    }

    @Test
    void clearedRaidCooldownsAreNotSaved() {
        UUID team = UUID.randomUUID();
        TerritorySavedData data = new TerritorySavedData();
        data.getRaidCooldowns().setVictimCooldownUntil(team, 1_000L);
        data.getRaidCooldowns().setVictimCooldownUntil(team, 0);

        CompoundTag tag = data.save(new CompoundTag());

        assertEquals(0, tag.getList("raidCooldowns", Tag.TAG_COMPOUND).size());
    }

    @Test
    void raidCooldownChangesMarkDataDirty() {
        TerritorySavedData data = new TerritorySavedData();

        data.getRaidCooldowns().setVictimCooldownUntil(UUID.randomUUID(), 1_000L);
        assertTrue(data.isDirty());

        data.setDirty(false);
        data.getRaidCooldowns().setRaiderCooldownUntil(UUID.randomUUID(), 1_000L);
        assertTrue(data.isDirty());
    }
}
