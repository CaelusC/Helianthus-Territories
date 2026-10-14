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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("purchase");
        CURRENCY_ITEM = builder
                .comment("Registry name of the item used to pay for regions, e.g. minecraft:diamond")
                .define("currencyItem", "minecraft:diamond",
                        value -> value instanceof String s && ResourceLocation.isValidResourceLocation(s));
        builder.pop();
        SPEC = builder.build();
    }

    private TerritoryConfig() {
    }
}
