package com.helianthus.territorywar.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RegionSyncCodecTest {
    @Test
    void roundTripsOwnedAndUnownedRegions() {
        Region unowned = new Region(RegionId.random(), "Plains", Set.of(new ChunkKey(1, -2, "minecraft:overworld"),
                new ChunkKey(3, 4, "minecraft:the_nether")), null, 12, true, 0x2E8B57);
        Region owned = new Region(RegionId.random(), "Hills", Set.of(new ChunkKey(0, 0, "minecraft:overworld")),
                UUID.randomUUID(), 5, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RegionSyncCodec.write(buf, List.of(unowned, owned));
        List<Region> read = RegionSyncCodec.read(buf);

        assertEquals(2, read.size());
        assertSameRegion(unowned, read.get(0));
        assertNull(read.get(0).getOwnerTeamId());
        assertSameRegion(owned, read.get(1));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void roundTripsCustomAndDefaultColor() {
        Region custom = new Region(RegionId.random(), "Coast", Set.of(new ChunkKey(9, 9, "minecraft:overworld")),
                null, 40, true, 0x1E90FF);
        Region plain = new Region(RegionId.random(), "Moor", Set.of(new ChunkKey(8, 9, "minecraft:overworld")),
                null, 40, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RegionSyncCodec.write(buf, List.of(custom, plain));
        List<Region> read = RegionSyncCodec.read(buf);

        assertEquals(0x1E90FF, read.get(0).getColorRgb());
        assertEquals(Region.DEFAULT_COLOR, read.get(1).getColorRgb());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void roundTripsEmptyList() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RegionSyncCodec.write(buf, List.of());

        assertTrue(RegionSyncCodec.read(buf).isEmpty());
    }

    private static void assertSameRegion(Region expected, Region actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getChunks(), actual.getChunks());
        assertEquals(expected.getOwnerTeamId(), actual.getOwnerTeamId());
        assertEquals(expected.getPurchasePrice(), actual.getPurchasePrice());
        assertEquals(expected.isForSale(), actual.isForSale());
        assertEquals(expected.getColorRgb(), actual.getColorRgb());
    }
}
