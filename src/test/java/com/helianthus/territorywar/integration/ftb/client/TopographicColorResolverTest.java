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
    void landIsSteppedIntoFourBlockContourBands() {
        assertEquals(TopographicColorResolver.landColor(96), TopographicColorResolver.landColor(99));
        assertTrue(TopographicColorResolver.landColor(99) != TopographicColorResolver.landColor(100));
        assertTrue(TopographicColorResolver.landColor(100) != TopographicColorResolver.landColor(104));
    }

    @Test
    void waterUsesDarkBrownDepthShades() {
        assertEquals(0x1F1610, TopographicColorResolver.SHALLOW_WATER);
        assertEquals(0x0C0805, TopographicColorResolver.DEEP_WATER);
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
