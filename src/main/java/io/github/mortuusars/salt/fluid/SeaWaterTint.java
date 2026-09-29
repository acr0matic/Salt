package io.github.mortuusars.salt.fluid;

public final class SeaWaterTint {
    public static final int DEFAULT_SEA_WATER_COLOR = 0x2E7D9E;
    public static final int DEFAULT_MARINE_BLEND_PERCENT = 35;
    public static final int DEFAULT_MARINE_TINT_RADIUS = 8;
    public static final int DEFAULT_OPACITY_PERCENT = 90;

    private SeaWaterTint() {
    }

    public static int apply(int waterColor) {
        return apply(waterColor, DEFAULT_SEA_WATER_COLOR, DEFAULT_MARINE_BLEND_PERCENT);
    }

    public static int apply(int waterColor, int seaWaterColor, int blendPercent) {
        int alpha = waterColor & 0xFF000000;
        if (alpha == 0)
            alpha = 0xFF000000;

        int red = blend(waterColor >> 16 & 0xFF, seaWaterColor >> 16 & 0xFF, blendPercent);
        int green = blend(waterColor >> 8 & 0xFF, seaWaterColor >> 8 & 0xFF, blendPercent);
        int blue = blend(waterColor & 0xFF, seaWaterColor & 0xFF, blendPercent);
        return alpha | red << 16 | green << 8 | blue;
    }

    public static int fluidColor(int seaWaterColor, int opacityPercent) {
        return opacityPercent * 255 / 100 << 24 | seaWaterColor;
    }

    public static int parseHexColor(String value, int fallback) {
        if (value == null)
            return fallback;

        try {
            return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16) & 0xFFFFFF;
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int blend(int waterChannel, int seaWaterChannel, int percent) {
        return waterChannel + (seaWaterChannel - waterChannel) * percent / 100;
    }
}
