package com.helianthus.territorywar.network;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/** Wire format of the full region list sent from server to client. */
public final class RegionSyncCodec {
    private RegionSyncCodec() {
    }

    public static void write(FriendlyByteBuf buf, Collection<Region> regions) {
        buf.writeVarInt(regions.size());
        for (Region region : regions) {
            buf.writeUUID(region.getId().value());
            buf.writeUtf(region.getName());
            buf.writeBoolean(region.isOwned());
            if (region.isOwned()) {
                buf.writeUUID(region.getOwnerTeamId());
            }
            buf.writeInt(region.getPurchasePrice());
            buf.writeBoolean(region.isForSale());
            buf.writeInt(region.getColorRgb());
            buf.writeVarInt(region.getChunks().size());
            for (ChunkKey chunk : region.getChunks()) {
                buf.writeInt(chunk.chunkX());
                buf.writeInt(chunk.chunkZ());
                buf.writeUtf(chunk.dimension());
            }
        }
    }

    public static List<Region> read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Region> regions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            RegionId id = new RegionId(buf.readUUID());
            String name = buf.readUtf();
            UUID owner = buf.readBoolean() ? buf.readUUID() : null;
            int price = buf.readInt();
            boolean forSale = buf.readBoolean();
            int color = buf.readInt();
            int chunkCount = buf.readVarInt();
            Set<ChunkKey> chunks = new HashSet<>();
            for (int j = 0; j < chunkCount; j++) {
                chunks.add(new ChunkKey(buf.readInt(), buf.readInt(), buf.readUtf()));
            }
            regions.add(new Region(id, name, chunks, owner, price, forSale, color));
        }
        return regions;
    }
}
