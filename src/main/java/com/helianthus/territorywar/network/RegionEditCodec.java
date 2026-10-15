package com.helianthus.territorywar.network;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

/** Wire format of an admin's requests, sent from the map, to delete a region or edit its chunks. */
public final class RegionEditCodec {
    private RegionEditCodec() {
    }

    /** Chunks to add to and remove from an existing region. */
    public record ChunkEdit(RegionId id, Set<ChunkKey> added, Set<ChunkKey> removed) {
        public ChunkEdit {
            Objects.requireNonNull(id, "id");
            added = Set.copyOf(added);
            removed = Set.copyOf(removed);
        }
    }

    public static void writeDelete(FriendlyByteBuf buf, RegionId id) {
        buf.writeUUID(id.value());
    }

    public static RegionId readDelete(FriendlyByteBuf buf) {
        return new RegionId(buf.readUUID());
    }

    public static void writeChunkEdit(FriendlyByteBuf buf, ChunkEdit edit) {
        buf.writeUUID(edit.id().value());
        writeChunks(buf, edit.added());
        writeChunks(buf, edit.removed());
    }

    public static ChunkEdit readChunkEdit(FriendlyByteBuf buf) {
        RegionId id = new RegionId(buf.readUUID());
        Set<ChunkKey> added = readChunks(buf);
        Set<ChunkKey> removed = readChunks(buf);
        return new ChunkEdit(id, added, removed);
    }

    private static void writeChunks(FriendlyByteBuf buf, Set<ChunkKey> chunks) {
        buf.writeVarInt(chunks.size());
        for (ChunkKey chunk : chunks) {
            buf.writeInt(chunk.chunkX());
            buf.writeInt(chunk.chunkZ());
            buf.writeUtf(chunk.dimension());
        }
    }

    private static Set<ChunkKey> readChunks(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<ChunkKey> chunks = new HashSet<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new ChunkKey(buf.readInt(), buf.readInt(), buf.readUtf()));
        }
        return chunks;
    }
}
