package com.helianthus.territorywar.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.TerritorySavedData;
import com.helianthus.territorywar.service.TerritoryService;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RegionEditHandlerTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void deletePacketFromAdminDeletesRegion() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        ChunkKey chunk = new ChunkKey(0, 0, OVERWORLD);
        RegionId id = service.createRegion("spawn", Set.of(chunk), 100, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RegionEditCodec.writeDelete(buf, id);

        RegionEditHandler.Result result = RegionEditHandler.delete(service, true, RegionEditCodec.readDelete(buf));

        assertEquals(RegionEditHandler.Result.SUCCESS, result);
        assertTrue(service.getRegion(id).isEmpty());
        assertTrue(service.getRegionAt(chunk).isEmpty());
    }

    @Test
    void deletePacketFromNonAdminIsIgnored() {
        TerritorySavedData data = new TerritorySavedData();
        TerritoryService service = new TerritoryService(data);
        RegionId id = service.createRegion("spawn", Set.of(new ChunkKey(0, 0, OVERWORLD)), 100, true);
        data.setDirty(false);

        assertEquals(RegionEditHandler.Result.NOT_ADMIN, RegionEditHandler.delete(service, false, id));

        assertTrue(service.getRegion(id).isPresent());
        assertFalse(data.isDirty());
    }

    @Test
    void deletePacketForUnknownRegionReportsIt() {
        TerritoryService service = new TerritoryService(new TerritorySavedData());

        assertEquals(RegionEditHandler.Result.UNKNOWN_REGION,
                RegionEditHandler.delete(service, true, RegionId.random()));
    }

    @Test
    void chunkEditFromAdminUpdatesRegion() {
        TerritoryService service = new TerritoryService(new TerritorySavedData());
        ChunkKey a = new ChunkKey(0, 0, OVERWORLD);
        ChunkKey b = new ChunkKey(1, 0, OVERWORLD);
        RegionId id = service.createRegion("spawn", Set.of(a), 100, true);

        RegionEditHandler.Result result = RegionEditHandler.editChunks(service, true,
                new RegionEditCodec.ChunkEdit(id, Set.of(b), Set.of(a)));

        assertEquals(RegionEditHandler.Result.SUCCESS, result);
        assertEquals(Set.of(b), service.getRegion(id).orElseThrow().getChunks());
    }

    @Test
    void chunkEditRejectsChunksOfAnotherRegion() {
        TerritoryService service = new TerritoryService(new TerritorySavedData());
        ChunkKey own = new ChunkKey(0, 0, OVERWORLD);
        ChunkKey other = new ChunkKey(3, 3, OVERWORLD);
        RegionId id = service.createRegion("spawn", Set.of(own), 100, true);
        RegionId otherId = service.createRegion("outpost", Set.of(other), 50, true);

        assertEquals(RegionEditHandler.Result.OVERLAP, RegionEditHandler.editChunks(service, true,
                new RegionEditCodec.ChunkEdit(id, Set.of(other), Set.of())));

        assertEquals(Optional.of(otherId), service.getRegionAt(other));
    }

    @Test
    void chunkEditRejectsRemovingEveryChunk() {
        TerritoryService service = new TerritoryService(new TerritorySavedData());
        ChunkKey a = new ChunkKey(0, 0, OVERWORLD);
        RegionId id = service.createRegion("spawn", Set.of(a), 100, true);

        assertEquals(RegionEditHandler.Result.NO_CHUNKS_LEFT, RegionEditHandler.editChunks(service, true,
                new RegionEditCodec.ChunkEdit(id, Set.of(), Set.of(a))));
        assertEquals(RegionEditHandler.Result.NOT_ADMIN, RegionEditHandler.editChunks(service, false,
                new RegionEditCodec.ChunkEdit(id, Set.of(), Set.of(a))));

        assertEquals(Set.of(a), service.getRegion(id).orElseThrow().getChunks());
    }
}
