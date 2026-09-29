package io.github.mortuusars.salt.fluid;

import io.github.mortuusars.salt.configuration.Configuration;

/** Клиентские визуальные настройки морской воды из salt-client.toml. */
public final class SeaWaterVisuals {
    private SeaWaterVisuals() {
    }

    public static int marineBlendPercent() {
        return Configuration.CLIENT.isLoaded()
                ? Configuration.SEA_WATER_MARINE_TINT_STRENGTH.get()
                : SeaWaterTint.DEFAULT_MARINE_BLEND_PERCENT;
    }

    public static int marineTintRadius() {
        return Configuration.CLIENT.isLoaded()
                ? Configuration.SEA_WATER_MARINE_TINT_RADIUS.get()
                : SeaWaterTint.DEFAULT_MARINE_TINT_RADIUS;
    }

    public static int seaWaterColor() {
        return SeaWaterTint.parseHexColor(
                Configuration.CLIENT.isLoaded() ? Configuration.SEA_WATER_TINT_COLOR.get() : null,
                SeaWaterTint.DEFAULT_SEA_WATER_COLOR);
    }

    public static int fluidColor() {
        int opacityPercent = Configuration.CLIENT.isLoaded()
                ? Configuration.SEA_WATER_OPACITY.get()
                : SeaWaterTint.DEFAULT_OPACITY_PERCENT;
        return SeaWaterTint.fluidColor(seaWaterColor(), opacityPercent);
    }
}
