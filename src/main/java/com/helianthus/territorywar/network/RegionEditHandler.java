package com.helianthus.territorywar.network;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.service.TerritoryService;
import java.util.Optional;

/** Server-side checks and effects of the map's region delete and chunk edit requests. */
final class RegionEditHandler {
    enum Result {
        SUCCESS,
        NOT_ADMIN,
        UNKNOWN_REGION,
        OVERLAP,
        NO_CHUNKS_LEFT
    }

    private RegionEditHandler() {
    }

    static Result delete(TerritoryService territory, boolean admin, RegionId id) {
        if (!admin) {
            return Result.NOT_ADMIN;
        }
        return territory.deleteRegion(id) ? Result.SUCCESS : Result.UNKNOWN_REGION;
    }

    static Result editChunks(TerritoryService territory, boolean admin, RegionEditCodec.ChunkEdit edit) {
        if (!admin) {
            return Result.NOT_ADMIN;
        }
        if (territory.getRegion(edit.id()).isEmpty()) {
            return Result.UNKNOWN_REGION;
        }
        for (ChunkKey chunk : edit.added()) {
            Optional<RegionId> existing = territory.getRegionAt(chunk);
            if (existing.isPresent() && !existing.get().equals(edit.id())) {
                return Result.OVERLAP;
            }
        }
        try {
            territory.editRegionChunks(edit.id(), edit.added(), edit.removed());
        } catch (IllegalArgumentException e) {
            return Result.NO_CHUNKS_LEFT;
        }
        return Result.SUCCESS;
    }
}
