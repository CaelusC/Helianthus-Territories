package com.helianthus.territorywar.integration.ftb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TopographicColorResolverTest {
    @Test
    void clampsHeightIntoPaletteRows() {
        assertEquals(0, TopographicColorResolver.paletteHeight(-64));
        assertEquals(0, TopographicColorResolver.paletteHeight(-32767));
        assertEquals(70, TopographicColorResolver.paletteHeight(70));
        assertEquals(255, TopographicColorResolver.paletteHeight(319));
    }

    @Test
    void landRunsFromLowlandToHighland() {
        assertEquals(TopographicColorResolver.LOWLAND, TopographicColorResolver.landColor(64));
        assertEquals(TopographicColorResolver.LOWLAND, TopographicColorResolver.landColor(10));
        assertEquals(TopographicColorResolver.HIGHLAND, TopographicColorResolver.landColor(200));
        assertEquals(TopographicColorResolver.HIGHLAND, TopographicColorResolver.landColor(255));
        assertTrue(brightness(TopographicColorResolver.landColor(80))
                < brightness(TopographicColorResolver.landColor(140)));
    }

    @Test
    void landIsSteppedIntoContourBands() {
        assertEquals(TopographicColorResolver.landColor(96), TopographicColorResolver.landColor(103));
        assertTrue(TopographicColorResolver.landColor(103) != TopographicColorResolver.landColor(104));
    }

    @Test
    void deeperWaterIsDarker() {
        assertEquals(TopographicColorResolver.SHALLOW_WATER, TopographicColorResolver.waterColor(63));
        assertEquals(TopographicColorResolver.DEEP_WATER, TopographicColorResolver.waterColor(0));
        assertTrue(brightness(TopographicColorResolver.waterColor(30))
                < brightness(TopographicColorResolver.waterColor(55)));
    }

    @Test
    void paletteHasLandThenWaterRows() {
        int[] palette = TopographicColorResolver.buildPalette();

        assertEquals(512, palette.length);
        assertEquals(TopographicColorResolver.landColor(70), palette[70]);
        assertEquals(TopographicColorResolver.waterColor(40), palette[256 + 40]);
    }

    private static int brightness(int rgb) {
        return (rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF);
    }
}
