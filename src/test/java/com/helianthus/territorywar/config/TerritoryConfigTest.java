package com.helianthus.territorywar.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;

class TerritoryConfigTest {
    @Test
    void raidSettingsHaveExpectedDefaults() {
        assertEquals(60, TerritoryConfig.RAID_WARMUP_MINUTES.getDefault());
        assertEquals(30, TerritoryConfig.SECONDS_PER_CHUNK.getDefault());
        assertEquals(60, TerritoryConfig.MAX_RAID_DURATION_MINUTES.getDefault());
        assertEquals(4, TerritoryConfig.MIN_ATTACKER_ONLINE.getDefault());
        assertEquals(2, TerritoryConfig.MIN_DEFENDER_ONLINE.getDefault());
        assertEquals(5, TerritoryConfig.VICTIM_COOLDOWN_DAYS.getDefault());
        assertEquals(7, TerritoryConfig.RAIDER_COOLDOWN_DAYS.getDefault());
        assertEquals("minecraft:block.end_portal.spawn", TerritoryConfig.GLOBAL_RAID_SOUND.getDefault());
    }

    @Test
    void emptyConfigIsFilledWithDefaultsUnderRaidSection() {
        CommentedConfig config = CommentedConfig.inMemory();

        TerritoryConfig.SPEC.correct(config);

        assertTrue(TerritoryConfig.SPEC.isCorrect(config));
        assertEquals(60, config.<Integer>get("raid.raidWarmupMinutes"));
        assertEquals(30, config.<Integer>get("raid.secondsPerChunk"));
        assertEquals(60, config.<Integer>get("raid.maxRaidDurationMinutes"));
        assertEquals(4, config.<Integer>get("raid.minAttackerOnline"));
        assertEquals(2, config.<Integer>get("raid.minDefenderOnline"));
        assertEquals(5, config.<Integer>get("raid.victimCooldownDays"));
        assertEquals(7, config.<Integer>get("raid.raiderCooldownDays"));
        assertEquals("minecraft:block.end_portal.spawn", config.<String>get("raid.globalRaidSound"));
    }

    @Test
    void validValuesAreKept() {
        CommentedConfig config = CommentedConfig.inMemory();
        TerritoryConfig.SPEC.correct(config);
        config.set("raid.raidWarmupMinutes", 0);
        config.set("raid.minAttackerOnline", 1);
        config.set("raid.globalRaidSound", "minecraft:entity.wither.spawn");

        assertTrue(TerritoryConfig.SPEC.isCorrect(config));
    }

    @Test
    void outOfRangeValuesAreClampedAndInvalidSoundIsReset() {
        CommentedConfig config = CommentedConfig.inMemory();
        TerritoryConfig.SPEC.correct(config);
        config.set("raid.raidWarmupMinutes", -1);
        config.set("raid.secondsPerChunk", 0);
        config.set("raid.maxRaidDurationMinutes", 0);
        config.set("raid.minAttackerOnline", -1);
        config.set("raid.minDefenderOnline", -1);
        config.set("raid.victimCooldownDays", -1);
        config.set("raid.raiderCooldownDays", -1);
        config.set("raid.globalRaidSound", "Not A Sound");

        assertFalse(TerritoryConfig.SPEC.isCorrect(config));
        TerritoryConfig.SPEC.correct(config);

        assertTrue(TerritoryConfig.SPEC.isCorrect(config));
        assertEquals(0, config.<Integer>get("raid.raidWarmupMinutes"));
        assertEquals(1, config.<Integer>get("raid.secondsPerChunk"));
        assertEquals(1, config.<Integer>get("raid.maxRaidDurationMinutes"));
        assertEquals(0, config.<Integer>get("raid.minAttackerOnline"));
        assertEquals(0, config.<Integer>get("raid.minDefenderOnline"));
        assertEquals(0, config.<Integer>get("raid.victimCooldownDays"));
        assertEquals(0, config.<Integer>get("raid.raiderCooldownDays"));
        assertEquals("minecraft:block.end_portal.spawn", config.<String>get("raid.globalRaidSound"));
    }
}
