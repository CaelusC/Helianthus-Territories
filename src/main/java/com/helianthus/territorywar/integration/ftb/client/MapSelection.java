package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.core.ChunkKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Rectangular chunk selection dragged on the FTB Chunks large map. */
final class MapSelection {
    /** Vanilla limit on a client-to-server custom payload. */
    static final int MAX_PAYLOAD_BYTES = 32767;
    /** Room left for the region name, price, flags, colour, chunk count and channel framing. */
    private static final int RESERVED_BYTES = 512;

    private MapSelection() {
    }

    /** Number of chunks in the rectangle spanned by two corners. */
    static long area(ChunkKey a, ChunkKey b) {
        return (Math.abs((long) a.chunkX() - b.chunkX()) + 1) * (Math.abs((long) a.chunkZ() - b.chunkZ()) + 1);
    }

    /** Every chunk in the rectangle spanned by two corners, in the first corner's dimension. */
    static Set<ChunkKey> rectangle(ChunkKey a, ChunkKey b) {
        Set<ChunkKey> chunks = new HashSet<>();
        for (int x = Math.min(a.chunkX(), b.chunkX()); x <= Math.max(a.chunkX(), b.chunkX()); x++) {
            for (int z = Math.min(a.chunkZ(), b.chunkZ()); z <= Math.max(a.chunkZ(), b.chunkZ()); z++) {
                chunks.add(new ChunkKey(x, z, a.dimension()));
            }
        }
        return chunks;
    }

    /** Most chunks of {@code dimension} that fit in one create region packet. */
    static int maxChunks(String dimension) {
        int dimensionBytes = dimension.getBytes(StandardCharsets.UTF_8).length;
        int perChunk = Integer.BYTES * 2 + varIntSize(dimensionBytes) + dimensionBytes;
        return (MAX_PAYLOAD_BYTES - RESERVED_BYTES) / perChunk;
    }

    private static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            size++;
        }
        return size;
    }
}
