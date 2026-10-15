package com.helianthus.territorywar.client;

import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.integration.ftb.client.RaidRegionScreen;
import com.helianthus.territorywar.integration.ftb.client.TerritoryScreen;
import com.helianthus.territorywar.network.CreateRegionCodec;
import com.helianthus.territorywar.network.RaidInfoCodec;
import com.helianthus.territorywar.network.RegionEditCodec;
import com.helianthus.territorywar.network.RegionSyncCodec;
import com.helianthus.territorywar.network.TerritoryNetwork;
import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/** Client side of {@link TerritoryNetwork}. Only call {@link #init} on a physical client. */
public final class TerritoryClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TerritoryClient() {
    }

    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TerritoryNetwork.SYNC_REGIONS, TerritoryClient::onSyncRegions);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TerritoryNetwork.OPEN_SCREEN, TerritoryClient::onOpenScreen);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TerritoryNetwork.RAID_INFO, TerritoryClient::onRaidInfo);
        MinecraftForge.EVENT_BUS.addListener(TerritoryClient::onLoggingOut);
        LOGGER.info("Registered territory client packet receivers");
    }

    /** Asks the server to buy the region for the player's team. */
    public static void requestPurchase(RegionId id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(id.value());
        NetworkManager.sendToServer(TerritoryNetwork.PURCHASE_REGION, buf);
    }

    /** Asks the server to create a region from chunks an admin selected. */
    public static void requestCreateRegion(CreateRegionCodec.Request request) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CreateRegionCodec.write(buf, request);
        NetworkManager.sendToServer(TerritoryNetwork.CREATE_REGION, buf);
    }

    /** Asks the server to delete a region. */
    public static void requestDeleteRegion(RegionId id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RegionEditCodec.writeDelete(buf, id);
        NetworkManager.sendToServer(TerritoryNetwork.DELETE_REGION, buf);
    }

    /** Asks the server to add chunks to or remove chunks from a region. */
    public static void requestEditRegionChunks(RegionEditCodec.ChunkEdit edit) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RegionEditCodec.writeChunkEdit(buf, edit);
        NetworkManager.sendToServer(TerritoryNetwork.EDIT_REGION_CHUNKS, buf);
    }

    /** Asks the server for the raid prompt contents of an enemy region. */
    public static void requestRaidInfo(RegionId id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(id.value());
        NetworkManager.sendToServer(TerritoryNetwork.REQUEST_RAID_INFO, buf);
    }

    /** Asks the server to declare a raid on the region for the player's team. */
    public static void requestDeclareRaid(RegionId id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(id.value());
        NetworkManager.sendToServer(TerritoryNetwork.DECLARE_RAID, buf);
    }

    private static void onSyncRegions(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        List<Region> regions = RegionSyncCodec.read(buf);
        context.queue(() -> {
            ClientRegionCache.setRegions(regions);
            LOGGER.debug("Received {} regions from server", regions.size());
            if (isScreenAvailable()) {
                TerritoryScreen.refreshIfOpen();
            }
        });
    }

    private static void onOpenScreen(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (isScreenAvailable()) {
                TerritoryScreen.open();
            } else {
                LOGGER.warn("Cannot open territory screen: FTB Library is not loaded");
            }
        });
    }

    private static void onRaidInfo(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        RaidInfoCodec.Info info = RaidInfoCodec.read(buf);
        context.queue(() -> {
            if (isScreenAvailable()) {
                RaidRegionScreen.open(info);
            }
        });
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRegionCache.clear();
    }

    private static boolean isScreenAvailable() {
        return ModList.get().isLoaded("ftblibrary");
    }
}
