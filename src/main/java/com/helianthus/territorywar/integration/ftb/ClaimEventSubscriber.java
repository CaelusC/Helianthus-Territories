package com.helianthus.territorywar.integration.ftb;

import com.mojang.logging.LogUtils;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Blocks manual FTB Chunks claiming by players (map screen and {@code /ftbchunks claim}) so that
 * territory is only acquired through mod regions. Claims from non-player sources pass through.
 */
public final class ClaimEventSubscriber {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Translation key used as the FTB custom claim problem id. */
    static final String MANUAL_CLAIM_DISABLED = "territorywar.claim.manual_disabled";

    private ClaimEventSubscriber() {
    }

    public static void register() {
        ClaimedChunkEvent.BEFORE_CLAIM.register(ClaimEventSubscriber::beforeClaim);
        LOGGER.info("Registered FTB Chunks manual claim blocker");
    }

    private static CompoundEventResult<ClaimResult> beforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return CompoundEventResult.pass();
        }
        LOGGER.debug("Blocked manual claim by {} at {}", player.getGameProfile().getName(), chunk.getPos());
        return CompoundEventResult.interruptFalse(ClaimResult.customProblem(MANUAL_CLAIM_DISABLED));
    }
}
