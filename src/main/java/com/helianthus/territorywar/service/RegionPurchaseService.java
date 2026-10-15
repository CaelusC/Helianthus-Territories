package com.helianthus.territorywar.service;

import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.integration.ftb.ChunkClaimIntegrationService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Sells regions to FTB teams. The buyer must lead their team and pay the region's price in
 * {@link #CURRENCY} from their inventory. The team then owns the region, it leaves the market, and
 * the team's FTB Chunks claim allowance grows by the region's chunk count.
 */
public final class RegionPurchaseService {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Placeholder purchase currency until it moves to the server config. */
    public static final Item CURRENCY = Items.DIAMOND;

    private final TerritoryService territory;
    private final TeamIntegrationService teams;
    private final ChunkClaimIntegrationService claims;

    public RegionPurchaseService(TerritoryService territory, TeamIntegrationService teams,
            ChunkClaimIntegrationService claims) {
        this.territory = Objects.requireNonNull(territory, "territory");
        this.teams = Objects.requireNonNull(teams, "teams");
        this.claims = Objects.requireNonNull(claims, "claims");
    }

    /**
     * Buys the region for the player's team. Returns why it failed, taking no payment, if the
     * region is unknown, owned or not for sale, the player does not lead a team, or they cannot
     * pay. A failed claim allowance update is logged but does not undo the purchase.
     */
    public PurchaseResult purchaseRegion(ServerPlayer player, RegionId regionId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(regionId, "regionId");
        String buyer = player.getGameProfile().getName();

        Optional<Region> found = territory.getRegion(regionId);
        Optional<UUID> teamId = teams.getPlayerTeamId(player);
        boolean teamLeader = teamId.isPresent() && teams.isTeamLeader(player, teamId.get());
        Inventory inventory = player.getInventory();
        PurchaseResult check = PurchaseResult.check(found, teamLeader, inventory.countItem(CURRENCY));
        if (check != PurchaseResult.SUCCESS) {
            LOGGER.debug("Purchase of region {} by {} rejected: {}", regionId.value(), buyer, check);
            return check;
        }
        Region region = found.get();
        int price = PurchaseResult.price(region);

        PurchaseResult completed = territory.completePurchase(regionId, teamId.get());
        if (completed != PurchaseResult.SUCCESS) {
            return completed;
        }
        if (price > 0) {
            ContainerHelper.clearOrCountMatchingItems(inventory, stack -> stack.is(CURRENCY), price, false);
            player.containerMenu.broadcastChanges();
        }

        int chunkCount = region.getChunks().size();
        if (!claims.addExtraClaimChunks(teamId.get(), chunkCount)) {
            LOGGER.warn("Region {} sold to team {} but its FTB Chunks claim allowance was not raised by {}",
                    regionId.value(), teamId.get(), chunkCount);
        }
        LOGGER.info("Region purchased: id={} name={} team={} buyer={} paid={} chunks={}",
                regionId.value(), region.getName(), teamId.get(), buyer, price, chunkCount);
        return PurchaseResult.SUCCESS;
    }
}
