package com.helianthus.territorywar.network;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

/** Wire format of an admin's request to create a region from chunks selected in the territory screen. */
public final class CreateRegionCodec {
    /** Longest region name accepted from the screen. */
    public static final int MAX_NAME_LENGTH = 64;

    private CreateRegionCodec() {
    }

    /** {@code colorRgb} is the unowned map colour, masked to its low 24 bits. */
    public record Request(String name, Set<ChunkKey> chunks, int price, boolean forSale, int colorRgb) {
        public Request {
            Objects.requireNonNull(name, "name");
            chunks = Set.copyOf(chunks);
            colorRgb &= 0xFFFFFF;
        }

        public Request(String name, Set<ChunkKey> chunks, int price, boolean forSale) {
            this(name, chunks, price, forSale, Region.DEFAULT_COLOR);
        }
    }

    public static void write(FriendlyByteBuf buf, Request request) {
        buf.writeUtf(request.name(), MAX_NAME_LENGTH);
        buf.writeInt(request.price());
        buf.writeBoolean(request.forSale());
        buf.writeInt(request.colorRgb());
        buf.writeVarInt(request.chunks().size());
        for (ChunkKey chunk : request.chunks()) {
            buf.writeInt(chunk.chunkX());
            buf.writeInt(chunk.chunkZ());
            buf.writeUtf(chunk.dimension());
        }
    }

    public static Request read(FriendlyByteBuf buf) {
        String name = buf.readUtf(MAX_NAME_LENGTH);
        int price = buf.readInt();
        boolean forSale = buf.readBoolean();
        int color = buf.readInt();
        int chunkCount = buf.readVarInt();
        Set<ChunkKey> chunks = new HashSet<>();
        for (int i = 0; i < chunkCount; i++) {
            chunks.add(new ChunkKey(buf.readInt(), buf.readInt(), buf.readUtf()));
        }
        return new Request(name, chunks, price, forSale, color);
    }
}
