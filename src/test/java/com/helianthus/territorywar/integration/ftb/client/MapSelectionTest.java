package com.helianthus.territorywar.integration.ftb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.network.CreateRegionCodec;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class MapSelectionTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void rectangleCoversBothCornersInAnyOrder() {
        ChunkKey a = new ChunkKey(2, -1, OVERWORLD);
        ChunkKey b = new ChunkKey(-1, 1, OVERWORLD);

        Set<ChunkKey> chunks = MapSelection.rectangle(a, b);

        assertEquals(12, chunks.size());
        assertEquals(12, MapSelection.area(a, b));
        assertEquals(chunks, MapSelection.rectangle(b, a));
        assertTrue(chunks.contains(a));
        assertTrue(chunks.contains(b));
        assertTrue(chunks.contains(new ChunkKey(0, 0, OVERWORLD)));
    }

    @Test
    void singleChunkSelection() {
        ChunkKey chunk = new ChunkKey(5, 5, OVERWORLD);

        assertEquals(Set.of(chunk), MapSelection.rectangle(chunk, chunk));
        assertEquals(1, MapSelection.area(chunk, chunk));
    }

    @Test
    void areaDoesNotOverflowForFarCorners() {
        ChunkKey a = new ChunkKey(-1_000_000, -1_000_000, OVERWORLD);
        ChunkKey b = new ChunkKey(1_000_000, 1_000_000, OVERWORLD);

        assertEquals(2_000_001L * 2_000_001L, MapSelection.area(a, b));
    }

    @Test
    void maxChunksSelectionFitsInOnePacket() {
        for (String dimension : new String[] {OVERWORLD, "somemod:" + "x".repeat(120)}) {
            int max = MapSelection.maxChunks(dimension);
            Set<ChunkKey> chunks = MapSelection.rectangle(new ChunkKey(0, 0, dimension),
                    new ChunkKey(max - 1, 0, dimension));
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

            CreateRegionCodec.write(buf, new CreateRegionCodec.Request("é".repeat(CreateRegionCodec.MAX_NAME_LENGTH),
                    chunks, Integer.MAX_VALUE, true, 0xFFC300));

            assertTrue(max > 0);
            assertTrue(buf.readableBytes() <= MapSelection.MAX_PAYLOAD_BYTES - 256,
                    () -> dimension + " encoded to " + buf.readableBytes() + " bytes");
        }
    }
}
