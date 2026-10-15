package com.helianthus.territorywar.network;

import com.helianthus.territorywar.core.RegionId;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

/** Wire format of what the raid prompt shows for an enemy region, sent by the server on request. */
public final class RaidInfoCodec {
    private RaidInfoCodec() {
    }

    /** Raid cost, online counts against their minimums, and warm-up length for one region. */
    public record Info(RegionId regionId, int price, int attackersOnline, int minAttackers, int defendersOnline,
            int minDefenders, int warmupMinutes) {
        public Info {
            Objects.requireNonNull(regionId, "regionId");
        }
    }

    public static void write(FriendlyByteBuf buf, Info info) {
        buf.writeUUID(info.regionId().value());
        buf.writeVarInt(info.price());
        buf.writeVarInt(info.attackersOnline());
        buf.writeVarInt(info.minAttackers());
        buf.writeVarInt(info.defendersOnline());
        buf.writeVarInt(info.minDefenders());
        buf.writeVarInt(info.warmupMinutes());
    }

    public static Info read(FriendlyByteBuf buf) {
        return new Info(new RegionId(buf.readUUID()), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
