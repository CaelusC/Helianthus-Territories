package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.core.Region;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Preset swatches and hex parsing for the admin region colour picker. */
final class RegionColors {
    /** Swatches offered in the create region dialog; the first is the default region colour. */
    static final int[] SWATCHES = {
        Region.DEFAULT_COLOR, 0xE53935, 0xFB8C00, 0x43A047, 0x00897B, 0x1E88E5, 0x8E24AA, 0x9E9E9E
    };
    /** Accepts partial input while typing. */
    static final String PARTIAL_HEX_PATTERN = "[0-9a-fA-F]{0,6}";

    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{6}");

    private RegionColors() {
    }

    /** RGB of a complete six-digit hex string, or empty if it is incomplete or invalid. */
    static OptionalInt parseHex(String text) {
        if (text == null || !HEX.matcher(text).matches()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(text, 16));
    }

    /** Six-digit upper-case hex of the low 24 bits of {@code rgb}. */
    static String toHex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }
}
