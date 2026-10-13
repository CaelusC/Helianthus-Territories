package com.helianthus.territorywar.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ChunkKeyTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void sameCoordinatesInDifferentDimensionsAreNotEqual() {
        assertNotEquals(new ChunkKey(3, -7, OVERWORLD), new ChunkKey(3, -7, NETHER));
    }

    @Test
    void sameCoordinatesAndDimensionAreEqual() {
        ChunkKey a = new ChunkKey(3, -7, OVERWORLD);
        ChunkKey b = new ChunkKey(3, -7, OVERWORLD);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void packedLongRoundTripsIncludingNegativeAndExtremeCoordinates() {
        int[][] coords = {{0, 0}, {3, -7}, {-1, -1}, {Integer.MAX_VALUE, Integer.MIN_VALUE}, {Integer.MIN_VALUE, Integer.MAX_VALUE}};
        for (int[] c : coords) {
            ChunkKey key = new ChunkKey(c[0], c[1], NETHER);
            assertEquals(key, ChunkKey.fromLong(key.toLong(), NETHER));
        }
    }

    @Test
    void packedLayoutPutsXInLowBitsAndZInHighBits() {
        assertEquals(0x0000000200000001L, new ChunkKey(1, 2, OVERWORLD).toLong());
    }
}
