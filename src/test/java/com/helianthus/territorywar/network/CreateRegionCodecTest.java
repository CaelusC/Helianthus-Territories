package com.helianthus.territorywar.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class CreateRegionCodecTest {
    @Test
    void roundTripsMultiChunkRequest() {
        CreateRegionCodec.Request request = new CreateRegionCodec.Request("Valley", Set.of(
                new ChunkKey(0, 0, "minecraft:overworld"), new ChunkKey(1, 0, "minecraft:overworld"),
                new ChunkKey(-5, 12, "minecraft:overworld")), 250, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        CreateRegionCodec.write(buf, request);
        CreateRegionCodec.Request read = CreateRegionCodec.read(buf);

        assertEquals(request, read);
        assertEquals(3, read.chunks().size());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void roundTripsSingleChunkForSaleRequest() {
        CreateRegionCodec.Request request = new CreateRegionCodec.Request("Plot",
                Set.of(new ChunkKey(7, -3, "minecraft:the_nether")), 0, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        CreateRegionCodec.write(buf, request);
        CreateRegionCodec.Request read = CreateRegionCodec.read(buf);

        assertEquals(request, read);
        assertTrue(read.forSale());
    }

    @Test
    void roundTripsColor() {
        CreateRegionCodec.Request request = new CreateRegionCodec.Request("Marsh",
                Set.of(new ChunkKey(2, 2, "minecraft:overworld")), 10, true, 0x8A2BE2);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        CreateRegionCodec.write(buf, request);
        CreateRegionCodec.Request read = CreateRegionCodec.read(buf);

        assertEquals(0x8A2BE2, read.colorRgb());
        assertEquals(request, read);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void colorDefaultsAndIsMaskedToRgb() {
        Set<ChunkKey> chunks = Set.of(new ChunkKey(0, 0, "minecraft:overworld"));

        assertEquals(Region.DEFAULT_COLOR, new CreateRegionCodec.Request("Plot", chunks, 0, true).colorRgb());
        assertEquals(0x123456, new CreateRegionCodec.Request("Plot", chunks, 0, true, 0xFF123456).colorRgb());
    }

    @Test
    void rejectsOverlongName() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("x".repeat(CreateRegionCodec.MAX_NAME_LENGTH + 1));

        assertThrows(DecoderException.class, () -> CreateRegionCodec.read(buf));
    }
}
