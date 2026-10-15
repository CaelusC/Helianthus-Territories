package com.helianthus.territorywar.network;

import com.helianthus.territorywar.TerritoryWar;
import com.helianthus.territorywar.command.TerritoryCommand;
import com.helianthus.territorywar.config.TerritoryConfig;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.TerritoryDataService;
import com.helianthus.territorywar.integration.ftb.ChunkClaimIntegrationService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RaidResult;
import com.helianthus.territorywar.service.RaidService;
import com.helianthus.territorywar.service.RegionPurchaseService;
import com.helianthus.territorywar.service.TerritoryManager;
import com.helianthus.territorywar.service.TerritoryService;
import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Architectury packets between server and client: {@link #SYNC_REGIONS}, {@link #OPEN_SCREEN} and
 * {@link #RAID_INFO} go to the client, {@link #PURCHASE_REGION}, {@link #CREATE_REGION},
 * {@link #DELETE_REGION}, {@link #EDIT_REGION_CHUNKS}, {@link #REQUEST_RAID_INFO} and {@link #DECLARE_RAID}
 * come from it. Client receivers are registered in {@code client.TerritoryClient}.
 */
public final class TerritoryNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** S2C: full region list, see {@link RegionSyncCodec}. */
    public static final ResourceLocation SYNC_REGIONS = new ResourceLocation(TerritoryWar.MODID, "sync_regions");
    /** S2C: no payload, opens the territory screen. */
    public static final ResourceLocation OPEN_SCREEN = new ResourceLocation(TerritoryWar.MODID, "open_screen");
    /** C2S: region id UUID to buy for the sender's team. */
    public static final ResourceLocation PURCHASE_REGION = new ResourceLocation(TerritoryWar.MODID, "purchase_region");
    /** C2S: admin request to create a region from selected chunks, see {@link CreateRegionCodec}. */
    public static final ResourceLocation CREATE_REGION =
            new ResourceLocation(TerritoryWar.MODID, "create_region_from_gui");
    /** C2S: admin request to delete a region, see {@link RegionEditCodec#writeDelete}. */
    public static final ResourceLocation DELETE_REGION = new ResourceLocation(TerritoryWar.MODID, "delete_region");
    /** C2S: admin request to add chunks to or remove chunks from a region, see {@link RegionEditCodec.ChunkEdit}. */
    public static final ResourceLocation EDIT_REGION_CHUNKS =
            new ResourceLocation(TerritoryWar.MODID, "edit_region_chunks");
    /** C2S: region id UUID of an enemy region to show the raid prompt for. */
    public static final ResourceLocation REQUEST_RAID_INFO =
            new ResourceLocation(TerritoryWar.MODID, "request_raid_info");
    /** S2C: what the raid prompt shows, see {@link RaidInfoCodec}. */
    public static final ResourceLocation RAID_INFO = new ResourceLocation(TerritoryWar.MODID, "raid_info");
    /** C2S: region id UUID to declare a raid on for the sender's team. */
    public static final ResourceLocation DECLARE_RAID = new ResourceLocation(TerritoryWar.MODID, "declare_raid");

    private TerritoryNetwork() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PURCHASE_REGION, TerritoryNetwork::onPurchaseRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CREATE_REGION, TerritoryNetwork::onCreateRegionRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DELETE_REGION, TerritoryNetwork::onDeleteRegionRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, EDIT_REGION_CHUNKS,
                TerritoryNetwork::onEditRegionChunksRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_RAID_INFO, TerritoryNetwork::onRaidInfoRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DECLARE_RAID, TerritoryNetwork::onDeclareRaidRequest);
        MinecraftForge.EVENT_BUS.addListener(TerritoryNetwork::onPlayerLoggedIn);
        LOGGER.info("Registered territory network packets");
    }

    /** Sends every region to the player. */
    public static void syncRegions(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, SYNC_REGIONS, regionsBuf(player.getServer()));
    }

    /** Sends every region to all online players, after any region change. */
    public static void syncRegionsToAll(MinecraftServer server) {
        NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), SYNC_REGIONS, regionsBuf(server));
    }

    /** Sends the current regions, then tells the client to open the territory screen. */
    public static void openScreen(ServerPlayer player) {
        syncRegions(player);
        NetworkManager.sendToPlayer(player, OPEN_SCREEN, new FriendlyByteBuf(Unpooled.buffer()));
    }

    private static FriendlyByteBuf regionsBuf(MinecraftServer server) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RegionSyncCodec.write(buf, TerritoryDataService.get(server).getRegions());
        return buf;
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncRegions(player);
        }
    }

    private static void onPurchaseRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegionId id = new RegionId(buf.readUUID());
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            TerritoryService territory = TerritoryManager.get(player.server);
            RegionPurchaseService purchases = new RegionPurchaseService(territory,
                    new TeamIntegrationService(), new ChunkClaimIntegrationService());
            PurchaseResult result = purchases.purchaseRegion(player, id);
            if (result != PurchaseResult.SUCCESS) {
                player.sendSystemMessage(TerritoryCommand.buyFailure(result, id, territory.getRegion(id), player)
                        .copy().withStyle(ChatFormatting.RED));
                return;
            }
            player.sendSystemMessage(Component.translatable("territorywar.command.buy.success", id.value().toString()));
            syncRegionsToAll(player.server);
        });
    }

    private static void onCreateRegionRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        CreateRegionCodec.Request request = CreateRegionCodec.read(buf);
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.hasPermissions(TerritoryCommand.ADMIN_PERMISSION_LEVEL)) {
                LOGGER.debug("Ignoring region creation request from non-admin {}", player.getGameProfile().getName());
                return;
            }
            if (request.name().isBlank() || request.chunks().isEmpty() || request.price() < 0) {
                LOGGER.debug("Ignoring invalid region creation request from {}", player.getGameProfile().getName());
                return;
            }
            TerritoryService territory = TerritoryManager.get(player.server);
            for (ChunkKey chunk : request.chunks()) {
                Optional<RegionId> existing = territory.getRegionAt(chunk);
                if (existing.isPresent()) {
                    player.sendSystemMessage(Component.translatable("territorywar.command.create.overlap",
                            chunk.chunkX(), chunk.chunkZ(), existing.get().value().toString())
                            .withStyle(ChatFormatting.RED));
                    return;
                }
            }
            RegionId id = territory.createRegion(request.name(), request.chunks(), request.price(),
                    request.forSale(), request.colorRgb());
            syncRegionsToAll(player.server);
            player.sendSystemMessage(Component.translatable("territorywar.command.create.success",
                    request.name(), id.value().toString()));
        });
    }

    private static void onDeleteRegionRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegionId id = RegionEditCodec.readDelete(buf);
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            RegionEditHandler.Result result = RegionEditHandler.delete(TerritoryManager.get(player.server),
                    player.hasPermissions(TerritoryCommand.ADMIN_PERMISSION_LEVEL), id);
            reportEdit(player, result, id, "territorywar.command.delete.success");
        });
    }

    private static void onEditRegionChunksRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegionEditCodec.ChunkEdit edit = RegionEditCodec.readChunkEdit(buf);
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            RegionEditHandler.Result result = RegionEditHandler.editChunks(TerritoryManager.get(player.server),
                    player.hasPermissions(TerritoryCommand.ADMIN_PERMISSION_LEVEL), edit);
            reportEdit(player, result, edit.id(), "territorywar.map.edit.success");
        });
    }

    /** Replies with the raid prompt contents if the region is owned by a team other than the sender's. */
    private static void onRaidInfoRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegionId id = new RegionId(buf.readUUID());
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            Optional<Region> region = TerritoryManager.get(player.server).getRegion(id);
            TeamIntegrationService teams = new TeamIntegrationService();
            Optional<UUID> teamId = teams.getPlayerTeamId(player);
            if (region.isEmpty() || !region.get().isOwned()
                    || teamId.map(region.get().getOwnerTeamId()::equals).orElse(false)) {
                LOGGER.debug("Ignoring raid info request for region {} from {}", id.value(),
                        player.getGameProfile().getName());
                return;
            }
            RaidInfoCodec.Info info = new RaidInfoCodec.Info(id, RaidResult.price(region.get()),
                    teamId.map(teams::getOnlineMemberCount).orElse(0), TerritoryConfig.MIN_ATTACKER_ONLINE.get(),
                    teams.getOnlineMemberCount(region.get().getOwnerTeamId()),
                    TerritoryConfig.MIN_DEFENDER_ONLINE.get(), TerritoryConfig.RAID_WARMUP_MINUTES.get());
            FriendlyByteBuf reply = new FriendlyByteBuf(Unpooled.buffer());
            RaidInfoCodec.write(reply, info);
            NetworkManager.sendToPlayer(player, RAID_INFO, reply);
        });
    }

    private static void onDeclareRaidRequest(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RegionId id = new RegionId(buf.readUUID());
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            TerritoryService territory = TerritoryManager.get(player.server);
            RaidResult result = RaidService.get().declareRaid(player, id, territory, new TeamIntegrationService(),
                    TerritoryDataService.get(player.server).getRaidCooldowns());
            if (result != RaidResult.SUCCESS) {
                player.sendSystemMessage(raidFailure(result, id, territory.getRegion(id), player)
                        .withStyle(ChatFormatting.RED));
            }
        });
    }

    /** Chat message explaining why a raid declaration failed. */
    private static MutableComponent raidFailure(RaidResult result, RegionId id, Optional<Region> region,
            ServerPlayer player) {
        String idText = id.value().toString();
        String name = region.map(Region::getName).orElse(idText);
        return switch (result) {
            case REGION_NOT_FOUND -> Component.translatable("territorywar.command.unknown_region", idText);
            case NOT_OWNED -> Component.translatable("territorywar.raid.not_owned", name);
            case ALREADY_RAIDED -> Component.translatable("territorywar.raid.already_raided", name);
            case NOT_TEAM_LEADER -> Component.translatable("territorywar.raid.not_team_leader", name);
            case OWN_REGION -> Component.translatable("territorywar.raid.own_region", name);
            case NOT_ENOUGH_ATTACKERS -> Component.translatable("territorywar.raid.not_enough_attackers",
                    TerritoryConfig.MIN_ATTACKER_ONLINE.get());
            case NOT_ENOUGH_DEFENDERS -> Component.translatable("territorywar.raid.not_enough_defenders",
                    TerritoryConfig.MIN_DEFENDER_ONLINE.get());
            case DEFENDER_ON_COOLDOWN -> Component.translatable("territorywar.raid.defender_on_cooldown", name);
            case ATTACKER_ON_COOLDOWN -> Component.translatable("territorywar.raid.attacker_on_cooldown");
            case INSUFFICIENT_FUNDS -> Component.translatable("territorywar.raid.insufficient_funds", name,
                    region.map(RaidResult::price).orElse(0), RegionPurchaseService.CURRENCY.getDescription(),
                    player.getInventory().countItem(RegionPurchaseService.CURRENCY));
            case SUCCESS -> throw new IllegalArgumentException("not a failure: " + result);
        };
    }

    private static void reportEdit(ServerPlayer player, RegionEditHandler.Result result, RegionId id,
            String successKey) {
        String regionId = id.value().toString();
        switch (result) {
            case SUCCESS -> {
                syncRegionsToAll(player.server);
                player.sendSystemMessage(Component.translatable(successKey, regionId));
            }
            case NOT_ADMIN -> LOGGER.debug("Ignoring region edit request from non-admin {}",
                    player.getGameProfile().getName());
            case UNKNOWN_REGION -> player.sendSystemMessage(Component.translatable(
                    "territorywar.command.unknown_region", regionId).withStyle(ChatFormatting.RED));
            case OVERLAP -> player.sendSystemMessage(Component.translatable(
                    "territorywar.map.edit.overlap", regionId).withStyle(ChatFormatting.RED));
            case NO_CHUNKS_LEFT -> player.sendSystemMessage(Component.translatable(
                    "territorywar.map.edit.no_chunks_left", regionId).withStyle(ChatFormatting.RED));
        }
    }
}
