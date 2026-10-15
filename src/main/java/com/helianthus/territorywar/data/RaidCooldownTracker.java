package com.helianthus.territorywar.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Per team raid cooldowns, stored as epoch millisecond timestamps until which the team may not be raided
 * (victim) or may not declare a raid (raider). A timestamp of {@code 0} means no cooldown.
 */
public final class RaidCooldownTracker {
    private static final String TAG_TEAM_ID = "teamId";
    private static final String TAG_VICTIM_UNTIL = "victimCooldownUntil";
    private static final String TAG_RAIDER_UNTIL = "raiderCooldownUntil";

    private record Cooldowns(long victimUntil, long raiderUntil) {
    }

    private final Map<UUID, Cooldowns> cooldowns = new HashMap<>();
    private final Runnable onChange;

    RaidCooldownTracker(Runnable onChange) {
        this.onChange = onChange;
    }

    public long getVictimCooldownUntil(UUID teamId) {
        Cooldowns entry = cooldowns.get(Objects.requireNonNull(teamId, "teamId"));
        return entry == null ? 0 : entry.victimUntil();
    }

    public long getRaiderCooldownUntil(UUID teamId) {
        Cooldowns entry = cooldowns.get(Objects.requireNonNull(teamId, "teamId"));
        return entry == null ? 0 : entry.raiderUntil();
    }

    public void setVictimCooldownUntil(UUID teamId, long until) {
        put(teamId, until, getRaiderCooldownUntil(teamId));
    }

    public void setRaiderCooldownUntil(UUID teamId, long until) {
        put(teamId, getVictimCooldownUntil(teamId), until);
    }

    private void put(UUID teamId, long victimUntil, long raiderUntil) {
        if (victimUntil <= 0 && raiderUntil <= 0) {
            cooldowns.remove(teamId);
        } else {
            cooldowns.put(teamId, new Cooldowns(victimUntil, raiderUntil));
        }
        onChange.run();
    }

    ListTag save() {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Cooldowns> entry : cooldowns.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(TAG_TEAM_ID, entry.getKey());
            tag.putLong(TAG_VICTIM_UNTIL, entry.getValue().victimUntil());
            tag.putLong(TAG_RAIDER_UNTIL, entry.getValue().raiderUntil());
            list.add(tag);
        }
        return list;
    }

    void load(ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.hasUUID(TAG_TEAM_ID)) {
                cooldowns.put(tag.getUUID(TAG_TEAM_ID),
                        new Cooldowns(tag.getLong(TAG_VICTIM_UNTIL), tag.getLong(TAG_RAIDER_UNTIL)));
            }
        }
    }
}
