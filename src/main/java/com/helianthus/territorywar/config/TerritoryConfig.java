package com.helianthus.territorywar.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server config, stored per world in {@code serverconfig/territorywar-server.toml}.
 */
public final class TerritoryConfig {
    public static final ForgeConfigSpec SPEC;

    /** Registry name of the item regions are paid for with. */
    public static final ForgeConfigSpec.ConfigValue<String> CURRENCY_ITEM;

    /** Minutes between declaring a raid and the raid starting. */
    public static final ForgeConfigSpec.IntValue RAID_WARMUP_MINUTES;
    /** Seconds attackers must hold a chunk to take it. */
    public static final ForgeConfigSpec.IntValue SECONDS_PER_CHUNK;
    /** Minutes after which a running raid ends. */
    public static final ForgeConfigSpec.IntValue MAX_RAID_DURATION_MINUTES;
    /** Attacking team members that must be online to declare a raid. */
    public static final ForgeConfigSpec.IntValue MIN_ATTACKER_ONLINE;
    /** Defending team members that must be online to declare a raid. */
    public static final ForgeConfigSpec.IntValue MIN_DEFENDER_ONLINE;
    /** Days a raided team is protected from new raids. */
    public static final ForgeConfigSpec.IntValue VICTIM_COOLDOWN_DAYS;
    /** Days a raiding team must wait before declaring another raid. */
    public static final ForgeConfigSpec.IntValue RAIDER_COOLDOWN_DAYS;
    /** Registry name of the sound played to everyone when a raid is announced. */
    public static final ForgeConfigSpec.ConfigValue<String> GLOBAL_RAID_SOUND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("purchase");
        CURRENCY_ITEM = builder
                .comment("Registry name of the item used to pay for regions, e.g. minecraft:diamond")
                .define("currencyItem", "minecraft:diamond",
                        value -> value instanceof String s && ResourceLocation.isValidResourceLocation(s));
        builder.pop();

        builder.push("raid");
        RAID_WARMUP_MINUTES = builder
                .comment("Minutes between declaring a raid and the raid starting")
                .defineInRange("raidWarmupMinutes", 60, 0, Integer.MAX_VALUE);
        SECONDS_PER_CHUNK = builder
                .comment("Seconds attackers must hold a chunk to take it")
                .defineInRange("secondsPerChunk", 30, 1, Integer.MAX_VALUE);
        MAX_RAID_DURATION_MINUTES = builder
                .comment("Minutes after which a running raid ends")
                .defineInRange("maxRaidDurationMinutes", 60, 1, Integer.MAX_VALUE);
        MIN_ATTACKER_ONLINE = builder
                .comment("Attacking team members that must be online to declare a raid")
                .defineInRange("minAttackerOnline", 4, 0, Integer.MAX_VALUE);
        MIN_DEFENDER_ONLINE = builder
                .comment("Defending team members that must be online to declare a raid")
                .defineInRange("minDefenderOnline", 2, 0, Integer.MAX_VALUE);
        VICTIM_COOLDOWN_DAYS = builder
                .comment("Days a raided team is protected from new raids")
                .defineInRange("victimCooldownDays", 5, 0, Integer.MAX_VALUE);
        RAIDER_COOLDOWN_DAYS = builder
                .comment("Days a raiding team must wait before declaring another raid")
                .defineInRange("raiderCooldownDays", 7, 0, Integer.MAX_VALUE);
        GLOBAL_RAID_SOUND = builder
                .comment("Registry name of the sound played to everyone when a raid is announced")
                .define("globalRaidSound", "minecraft:block.end_portal.spawn",
                        value -> value instanceof String s && ResourceLocation.isValidResourceLocation(s));
        builder.pop();
        SPEC = builder.build();
    }

    private TerritoryConfig() {
    }
}
