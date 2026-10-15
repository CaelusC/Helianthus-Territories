package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.RegionId;
import java.util.List;
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
}
