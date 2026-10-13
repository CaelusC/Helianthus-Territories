package com.helianthus.territorywar.integration.ftb;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Server-side team queries backed by FTB Teams. Every query returns an empty/false result when FTB
 * Teams is not loaded or its manager is not yet available. FTB types are only touched from
 * {@link Ftb}, so this class stays loadable without the mod present.
 */
public final class TeamIntegrationService {
    private static final String FTB_TEAMS_MOD_ID = "ftbteams";

    /** Returns the id of the team the player is currently in, if FTB Teams is available. */
    public Optional<UUID> getPlayerTeamId(ServerPlayer player) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        return Ftb.getPlayerTeamId(player);
    }

    /** Returns true if the player is an officer or the owner of the given team. */
    public boolean isTeamLeader(ServerPlayer player, UUID teamId) {
        if (!isAvailable()) {
            return false;
        }
        return Ftb.isTeamLeader(player, teamId);
    }

    private static boolean isAvailable() {
        return ModList.get().isLoaded(FTB_TEAMS_MOD_ID) && Ftb.isManagerLoaded();
    }

    private static final class Ftb {
        private Ftb() {
        }

        static boolean isManagerLoaded() {
            return FTBTeamsAPI.api().isManagerLoaded();
        }

        static Optional<UUID> getPlayerTeamId(ServerPlayer player) {
            return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).map(Team::getId);
        }

        static boolean isTeamLeader(ServerPlayer player, UUID teamId) {
            return FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                    .map(team -> team.getRankForPlayer(player.getUUID()).isOfficerOrBetter())
                    .orElse(false);
        }
    }
}
