package com.helianthus.territorywar.service;

import com.helianthus.territorywar.core.Region;
import java.util.Optional;
import java.util.UUID;

/** Outcome of declaring a raid on an enemy region. Every value other than {@link #SUCCESS} leaves state unchanged. */
public enum RaidResult {
    SUCCESS,
    REGION_NOT_FOUND,
    NOT_OWNED,
    ALREADY_RAIDED,
    NOT_TEAM_LEADER,
    OWN_REGION,
    NOT_ENOUGH_ATTACKERS,
    NOT_ENOUGH_DEFENDERS,
    DEFENDER_ON_COOLDOWN,
    ATTACKER_ON_COOLDOWN,
    INSUFFICIENT_FUNDS;

    /** Cost of raiding the region in purchase currency: half its price, at least 1. */
    public static int price(Region region) {
        return Math.max(1, region.getPurchasePrice() / 2);
    }

    /** Returns {@link #SUCCESS} if the region exists, is owned by a team and is not already being raided. */
    static RaidResult target(Optional<Region> region, boolean alreadyRaided) {
        if (region.isEmpty()) {
            return REGION_NOT_FOUND;
        }
        if (!region.get().isOwned()) {
            return NOT_OWNED;
        }
        if (alreadyRaided) {
            return ALREADY_RAIDED;
        }
        return SUCCESS;
    }

    /** Returns {@link #SUCCESS} if the player leads a team other than the region's owner. */
    static RaidResult attacker(Region region, Optional<UUID> attackerTeamId, boolean teamLeader) {
        if (attackerTeamId.isEmpty() || !teamLeader) {
            return NOT_TEAM_LEADER;
        }
        if (attackerTeamId.get().equals(region.getOwnerTeamId())) {
            return OWN_REGION;
        }
        return SUCCESS;
    }

    /** Returns {@link #SUCCESS} if both teams have at least their required number of members online. */
    static RaidResult online(int attackersOnline, int minAttackers, int defendersOnline, int minDefenders) {
        if (attackersOnline < minAttackers) {
            return NOT_ENOUGH_ATTACKERS;
        }
        if (defendersOnline < minDefenders) {
            return NOT_ENOUGH_DEFENDERS;
        }
        return SUCCESS;
    }

    /**
     * Returns {@link #SUCCESS} if neither the defender's victim cooldown nor the attacker's raider cooldown,
     * both epoch millisecond timestamps, is still running at {@code now}.
     */
    static RaidResult cooldowns(long defenderVictimUntil, long attackerRaiderUntil, long now) {
        if (now < defenderVictimUntil) {
            return DEFENDER_ON_COOLDOWN;
        }
        if (now < attackerRaiderUntil) {
            return ATTACKER_ON_COOLDOWN;
        }
        return SUCCESS;
    }
}
