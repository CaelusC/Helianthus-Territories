package com.helianthus.territorywar;

import com.helianthus.territorywar.command.TerritoryCommand;
import com.helianthus.territorywar.config.TerritoryConfig;
import com.helianthus.territorywar.integration.ftb.ClaimEventSubscriber;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TerritoryWar.MODID)
public class TerritoryWar
{
    public static final String MODID = "territorywar";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TerritoryWar()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TerritoryConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(TerritoryCommand::onRegisterCommands);

        if (ModList.get().isLoaded("ftbchunks"))
        {
            ClaimEventSubscriber.register();
        }
    }
}
