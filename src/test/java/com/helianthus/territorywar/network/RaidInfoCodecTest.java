package com.helianthus.territorywar.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helianthus.territorywar.core.RegionId;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RaidInfoCodecTest {
    @Test
    void roundTripsInfo() {
        RaidInfoCodec.Info info = new RaidInfoCodec.Info(RegionId.random(), 50, 3, 4, 2, 2, 90);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RaidInfoCodec.write(buf, info);

        assertEquals(info, RaidInfoCodec.read(buf));
        assertEquals(0, buf.readableBytes());
    }
}
