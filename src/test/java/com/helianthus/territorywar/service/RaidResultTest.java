package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RaidResultTest {
    private static final Set<ChunkKey> CHUNKS = Set.of(new ChunkKey(0, 0, "minecraft:overworld"));
    private static final long NOW = 1_700_000_000_000L;

    private static Region region(UUID owner, int price) {
        return new Region(RegionId.random(), "spawn", CHUNKS, owner, price, false);
    }

    @Test
    void priceIsHalfRoundedDown() {
        assertEquals(50, RaidResult.price(region(UUID.randomUUID(), 100)));
        assertEquals(50, RaidResult.price(region(UUID.randomUUID(), 101)));
    }

    @Test
    void priceIsAtLeastOne() {
        assertEquals(1, RaidResult.price(region(UUID.randomUUID(), 1)));
        assertEquals(1, RaidResult.price(region(UUID.randomUUID(), 0)));
    }

    @Test
    void unknownRegionIsNotFound() {
        assertEquals(RaidResult.REGION_NOT_FOUND, RaidResult.target(Optional.empty(), false));
    }

    @Test
    void unownedRegionCannotBeRaided() {
        assertEquals(RaidResult.NOT_OWNED, RaidResult.target(Optional.of(region(null, 10)), false));
    }

    @Test
    void regionAlreadyRaidedIsRejected() {
        assertEquals(RaidResult.ALREADY_RAIDED, RaidResult.target(Optional.of(region(UUID.randomUUID(), 10)), true));
    }

    @Test
    void ownedRegionIsValidTarget() {
        assertEquals(RaidResult.SUCCESS, RaidResult.target(Optional.of(region(UUID.randomUUID(), 10)), false));
    }

    @Test
    void playerWithoutTeamOrLeadershipIsRejected() {
        Region region = region(UUID.randomUUID(), 10);
        assertEquals(RaidResult.NOT_TEAM_LEADER, RaidResult.attacker(region, Optional.empty(), true));
        assertEquals(RaidResult.NOT_TEAM_LEADER, RaidResult.attacker(region, Optional.of(UUID.randomUUID()), false));
    }

    @Test
    void teamCannotRaidItsOwnRegion() {
        UUID team = UUID.randomUUID();
        assertEquals(RaidResult.OWN_REGION, RaidResult.attacker(region(team, 10), Optional.of(team), true));
    }

    @Test
    void leaderOfOtherTeamMayRaid() {
        assertEquals(RaidResult.SUCCESS,
                RaidResult.attacker(region(UUID.randomUUID(), 10), Optional.of(UUID.randomUUID()), true));
    }

    @Test
    void tooFewAttackersOnlineIsRejected() {
        assertEquals(RaidResult.NOT_ENOUGH_ATTACKERS, RaidResult.online(3, 4, 2, 2));
    }

    @Test
    void tooFewDefendersOnlineIsRejected() {
        assertEquals(RaidResult.NOT_ENOUGH_DEFENDERS, RaidResult.online(4, 4, 1, 2));
    }

    @Test
    void attackersAreCheckedBeforeDefenders() {
        assertEquals(RaidResult.NOT_ENOUGH_ATTACKERS, RaidResult.online(0, 4, 0, 2));
    }

    @Test
    void exactlyTheMinimumOnlineIsEnough() {
        assertEquals(RaidResult.SUCCESS, RaidResult.online(4, 4, 2, 2));
        assertEquals(RaidResult.SUCCESS, RaidResult.online(0, 0, 0, 0));
    }

    @Test
    void defenderOnVictimCooldownIsRejected() {
        assertEquals(RaidResult.DEFENDER_ON_COOLDOWN, RaidResult.cooldowns(NOW + 1, 0, NOW));
    }

    @Test
    void attackerOnRaiderCooldownIsRejected() {
        assertEquals(RaidResult.ATTACKER_ON_COOLDOWN, RaidResult.cooldowns(0, NOW + 1, NOW));
    }

    @Test
    void defenderCooldownIsCheckedFirst() {
        assertEquals(RaidResult.DEFENDER_ON_COOLDOWN, RaidResult.cooldowns(NOW + 1, NOW + 1, NOW));
    }

    @Test
    void expiredOrMissingCooldownsAllowRaid() {
        assertEquals(RaidResult.SUCCESS, RaidResult.cooldowns(0, 0, NOW));
        assertEquals(RaidResult.SUCCESS, RaidResult.cooldowns(NOW, NOW, NOW));
        assertEquals(RaidResult.SUCCESS, RaidResult.cooldowns(NOW - 1, NOW - 1, NOW));
    }
}
