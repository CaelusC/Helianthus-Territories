package com.helianthus.territorywar.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RegionEditCodecTest {
    @Test
    void roundTripsDelete() {
        RegionId id = RegionId.random();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RegionEditCodec.writeDelete(buf, id);

        assertEquals(id, RegionEditCodec.readDelete(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void roundTripsChunkEdit() {
        RegionEditCodec.ChunkEdit edit = new RegionEditCodec.ChunkEdit(RegionId.random(),
                Set.of(new ChunkKey(1, 2, "minecraft:overworld"), new ChunkKey(-3, 4, "minecraft:overworld")),
                Set.of(new ChunkKey(9, 9, "minecraft:the_nether")));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RegionEditCodec.writeChunkEdit(buf, edit);
        RegionEditCodec.ChunkEdit read = RegionEditCodec.readChunkEdit(buf);

        assertEquals(edit, read);
        assertEquals(0, buf.readableBytes());
    }
}
