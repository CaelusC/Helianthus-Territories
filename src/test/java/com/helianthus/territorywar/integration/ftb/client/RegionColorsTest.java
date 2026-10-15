package com.helianthus.territorywar.integration.ftb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helianthus.territorywar.core.Region;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class RegionColorsTest {
    @Test
    void parsesCompleteHexInAnyCase() {
        assertEquals(OptionalInt.of(0xFFC300), RegionColors.parseHex("FFC300"));
        assertEquals(OptionalInt.of(0x1e90ff), RegionColors.parseHex("1e90ff"));
    }

    @Test
    void rejectsIncompleteOrInvalidHex() {
        assertTrue(RegionColors.parseHex("").isEmpty());
        assertTrue(RegionColors.parseHex("FFC30").isEmpty());
        assertTrue(RegionColors.parseHex("GGGGGG").isEmpty());
        assertTrue(RegionColors.parseHex("#FFC300").isEmpty());
        assertTrue(RegionColors.parseHex(null).isEmpty());
    }

    @Test
    void formatsAndRoundTripsHex() {
        assertEquals("00FF0A", RegionColors.toHex(0xFF00FF0A));
        for (int swatch : RegionColors.SWATCHES) {
            assertEquals(OptionalInt.of(swatch), RegionColors.parseHex(RegionColors.toHex(swatch)));
        }
    }

    @Test
    void firstSwatchIsDefaultColor() {
        assertEquals(Region.DEFAULT_COLOR, RegionColors.SWATCHES[0]);
    }
}
