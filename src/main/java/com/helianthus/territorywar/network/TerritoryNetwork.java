package com.helianthus.territorywar.network;

import com.helianthus.territorywar.TerritoryWar;
import com.helianthus.territorywar.command.TerritoryCommand;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.TerritoryDataService;
import com.helianthus.territorywar.integration.ftb.ChunkClaimIntegrationService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RegionPurchaseService;
import com.helianthus.territorywar.service.TerritoryManager;
import com.helianthus.territorywar.service.TerritoryService;
import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Architectury packets between server and client: {@link #SYNC_REGIONS} and {@link #OPEN_SCREEN}
 * go to the client, {@link #PURCHASE_REGION} and {@link #CREATE_REGION} come from it. Client
 * receivers are registered in {@code client.TerritoryClient}.
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

    private TerritoryNetwork() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PURCHASE_REGION, TerritoryNetwork::onPurchaseRequest);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CREATE_REGION, TerritoryNetwork::onCreateRegionRequest);
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
}
