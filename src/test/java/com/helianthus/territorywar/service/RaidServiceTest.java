package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RaidServiceTest {
    @Test
    void formatsWarmupAsHours() {
        assertEquals("1", RaidService.formatHours(60));
        assertEquals("1.5", RaidService.formatHours(90));
        assertEquals("0.5", RaidService.formatHours(30));
        assertEquals("2", RaidService.formatHours(120));
        assertEquals("0", RaidService.formatHours(0));
    }

    @Test
    void warmupBecomesActiveOnceItEnds() {
        RaidService raids = new RaidService();
        RegionId region = RegionId.random();
        UUID attacker = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        raids.startWarmup(region, attacker, defender, 1_000L);

        RaidService.Raid pending = raids.getRaid(region).orElseThrow();
        assertEquals(RaidService.State.WARMUP, pending.state());
        assertEquals(attacker, pending.attackerTeamId());
        assertEquals(defender, pending.defenderTeamId());
        assertTrue(raids.startDueRaids(999L).isEmpty());
        assertEquals(RaidService.State.WARMUP, raids.getRaid(region).orElseThrow().state());

        List<RaidService.Raid> started = raids.startDueRaids(1_000L);

        assertEquals(1, started.size());
        assertEquals(RaidService.State.ACTIVE, raids.getRaid(region).orElseThrow().state());
        assertTrue(raids.startDueRaids(2_000L).isEmpty());
    }

    @Test
    void clearForgetsAllRaids() {
        RaidService raids = new RaidService();
        RegionId region = RegionId.random();
        raids.startWarmup(region, UUID.randomUUID(), UUID.randomUUID(), 1_000L);

        raids.clear();

        assertTrue(raids.getRaid(region).isEmpty());
    }

    @Test
    void regionCenterIsMiddleOfChunkBoundingBox() {
        String overworld = "minecraft:overworld";

        assertEquals(new RaidService.Center(8, 8), RaidService.regionCenter(Set.of(new ChunkKey(0, 0, overworld))));
        assertEquals(new RaidService.Center(16, 8), RaidService.regionCenter(
                Set.of(new ChunkKey(0, 0, overworld), new ChunkKey(1, 0, overworld))));
        assertEquals(new RaidService.Center(0, 24), RaidService.regionCenter(
                Set.of(new ChunkKey(-2, 0, overworld), new ChunkKey(1, 2, overworld))));
        assertThrows(IllegalArgumentException.class, () -> RaidService.regionCenter(Set.of()));
    }

    @Test
    void controlZoneIsHorizontalRadiusAroundCenter() {
        RaidService.Center center = new RaidService.Center(100, -50);

        assertTrue(RaidService.inControlZone(center, 100, -50));
        assertTrue(RaidService.inControlZone(center, 140, -50));
        assertTrue(RaidService.inControlZone(center, 100, -90));
        assertFalse(RaidService.inControlZone(center, 140.1, -50));
        assertFalse(RaidService.inControlZone(center, 130, -80));
    }

    @Test
    void holdSecondsScalesWithChunksUpToMaxDuration() {
        assertEquals(30, RaidService.holdSeconds(1, 30, 60));
        assertEquals(300, RaidService.holdSeconds(10, 30, 60));
        assertEquals(3600, RaidService.holdSeconds(120, 30, 60));
        assertEquals(3600, RaidService.holdSeconds(500, 30, 60));
        assertEquals((long) Integer.MAX_VALUE * 60,
                RaidService.holdSeconds(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void containerUseIsBlockedForOutsidersDuringActiveRaid() {
        UUID attacker = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        Optional<RaidService.Raid> active = Optional.of(
                new RaidService.Raid(RegionId.random(), attacker, defender, RaidService.State.ACTIVE, 0L));

        assertTrue(RaidService.blocksContainerUse(active, true, Optional.of(attacker)));
        assertTrue(RaidService.blocksContainerUse(active, true, Optional.of(UUID.randomUUID())));
        assertTrue(RaidService.blocksContainerUse(active, true, Optional.empty()));
        assertFalse(RaidService.blocksContainerUse(active, true, Optional.of(defender)));
    }

    @Test
    void containerUseIsAllowedForNonStorageOrWithoutActiveRaid() {
        UUID attacker = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        Optional<RaidService.Raid> active = Optional.of(
                new RaidService.Raid(RegionId.random(), attacker, defender, RaidService.State.ACTIVE, 0L));
        Optional<RaidService.Raid> warmup = Optional.of(
                new RaidService.Raid(RegionId.random(), attacker, defender, RaidService.State.WARMUP, 0L));

        assertFalse(RaidService.blocksContainerUse(active, false, Optional.of(attacker)));
        assertFalse(RaidService.blocksContainerUse(warmup, true, Optional.of(attacker)));
        assertFalse(RaidService.blocksContainerUse(Optional.empty(), true, Optional.of(attacker)));
    }
}
