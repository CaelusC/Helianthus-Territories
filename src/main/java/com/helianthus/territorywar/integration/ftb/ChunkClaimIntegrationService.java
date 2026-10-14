package com.helianthus.territorywar.integration.ftb;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Server-side FTB Chunks claim allowance updates. Every call returns {@code false} when FTB Teams
 * or FTB Chunks is not loaded or its manager is not yet available. FTB types are only touched from
 * {@link Ftb}, so this class stays loadable without the mods present.
 */
public final class ChunkClaimIntegrationService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FTB_TEAMS_MOD_ID = "ftbteams";
    private static final String FTB_CHUNKS_MOD_ID = "ftbchunks";

    /**
     * Raises the team's extra claim allowance by {@code amount}. The bonus is stored on the team
     * owner's personal data, which counts towards the team limit in every FTB party limit mode.
     * Returns {@code false}, leaving FTB state unchanged, if the team or its data is not found.
     */
    public boolean addExtraClaimChunks(UUID teamId, int amount) {
        Objects.requireNonNull(teamId, "teamId");
        if (!isAvailable()) {
            return false;
        }
        return Ftb.addExtraClaimChunks(teamId, amount);
    }

    private static boolean isAvailable() {
        ModList mods = ModList.get();
        return mods.isLoaded(FTB_TEAMS_MOD_ID) && mods.isLoaded(FTB_CHUNKS_MOD_ID) && Ftb.areManagersLoaded();
    }

    private static final class Ftb {
        private Ftb() {
        }

        static boolean areManagersLoaded() {
            return FTBTeamsAPI.api().isManagerLoaded() && FTBChunksAPI.api().isManagerLoaded();
        }

        /** Mirrors FTB's {@code /ftbchunks extra_claim_chunks}; see docs/ftb-api-notes.md. */
        static boolean addExtraClaimChunks(UUID teamId, int amount) {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamByID(teamId);
            if (team.isEmpty()) {
                LOGGER.debug("No FTB team {} to add extra claim chunks to", teamId);
                return false;
            }
            ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
            ChunkTeamData personal = manager.getPersonalData(team.get().getOwner());
            ChunkTeamData teamData = manager.getOrCreateData(team.get());
            if (!(personal instanceof ChunkTeamDataImpl personalImpl)
                    || !(teamData instanceof ChunkTeamDataImpl teamImpl)) {
                LOGGER.debug("No FTB Chunks data for team {} owner {}", teamId, team.get().getOwner());
                return false;
            }
            int extra = personalImpl.getExtraClaimChunks() + amount;
            personalImpl.setExtraClaimChunks(extra);
            personalImpl.markDirty();
            teamImpl.updateLimits();
            SendGeneralDataPacket.send(teamData, team.get().getOnlineMembers());
            LOGGER.debug("Extra claim chunks for team {} owner {} set to {}", teamId, team.get().getOwner(), extra);
            return true;
        }
    }
}
