package com.helianthus.territorywar.integration.ftb.client;

import dev.ftb.mods.ftblibrary.icon.Color4I;

/**
 * Brown topographic palette for the FTB Chunks map. Land is shaded by surface height in 4-block
 * contour bands from a dark umber to a faded parchment; water is shaded by depth from a dark brown
 * to a near-black brown. Laid out like FTB's own topography palette: index {@code y} for land and
 * {@code 256 + y} for water, where {@code y} is the surface (or sea floor) height clamped to 0..255.
 */
public final class TopographicColorResolver {
    public static final int PALETTE_HEIGHT = 256;
    public static final int SEA_LEVEL = 63;

    static final int LOWLAND = 0x6B5D4A;
    static final int HIGHLAND = 0xCBBFA3;
    static final int SHALLOW_WATER = 0x1F1610;
    static final int DEEP_WATER = 0x0C0805;

    private static final int LOWLAND_Y = SEA_LEVEL + 1;
    private static final int HIGHLAND_Y = 200;
    private static final int LAND_BAND = 4;
    private static final int WATER_BAND = 4;
    private static final int MAX_DEPTH = 48;

    private static Color4I[] palette;

    private TopographicColorResolver() {
    }

    /** Clamps a stored map height into a valid palette row. */
    public static int paletteHeight(int y) {
        return Math.max(0, Math.min(PALETTE_HEIGHT - 1, y));
    }

    /** RGB for a land surface at height {@code y}, stepped into contour bands. */
    public static int landColor(int y) {
        int banded = Math.floorDiv(y, LAND_BAND) * LAND_BAND;
        return lerp(LOWLAND, HIGHLAND, (float) (banded - LOWLAND_Y) / (HIGHLAND_Y - LOWLAND_Y));
    }

    /** RGB for water whose floor sits at height {@code floorY}; deeper water is darker. */
    public static int waterColor(int floorY) {
        int depth = Math.max(0, SEA_LEVEL - floorY);
        int banded = depth / WATER_BAND * WATER_BAND;
        return lerp(SHALLOW_WATER, DEEP_WATER, (float) banded / MAX_DEPTH);
    }

    /** Full 512-entry palette as RGB: land rows first, then water rows. */
    public static int[] buildPalette() {
        int[] colors = new int[PALETTE_HEIGHT * 2];
        for (int y = 0; y < PALETTE_HEIGHT; y++) {
            colors[y] = landColor(y);
            colors[PALETTE_HEIGHT + y] = waterColor(y);
        }
        return colors;
    }

    /** Palette as opaque FTB colours, built once and reused by every map render task. */
    public static Color4I[] getPalette() {
        if (palette == null) {
            int[] colors = buildPalette();
            Color4I[] built = new Color4I[colors.length];
            for (int i = 0; i < colors.length; i++) {
                built[i] = Color4I.rgba(0xFF000000 | colors[i]);
            }
            palette = built;
        }
        return palette;
    }

    static int lerp(int from, int to, float t) {
        float f = Math.max(0F, Math.min(1F, t));
        int r = channel(from >> 16, to >> 16, f);
        int g = channel(from >> 8, to >> 8, f);
        int b = channel(from, to, f);
        return r << 16 | g << 8 | b;
    }

    private static int channel(int from, int to, float t) {
        int a = from & 0xFF;
        int b = to & 0xFF;
        return Math.round(a + (b - a) * t);
    }
}
