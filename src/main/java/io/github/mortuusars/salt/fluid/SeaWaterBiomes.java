package io.github.mortuusars.salt.fluid;

import io.github.mortuusars.salt.Salt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;

public final class SeaWaterBiomes {
    private SeaWaterBiomes() {
    }

    public static boolean isFallbackSeaBiome(String biomeId) {
        int separator = biomeId.indexOf(':');
        String path = separator >= 0 ? biomeId.substring(separator + 1) : biomeId;
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return normalizedPath.contains("ocean")
                || normalizedPath.contains("sea")
                || normalizedPath.contains("reef")
                || normalizedPath.contains("beach");
    }

    public static boolean isSeaBiome(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(Salt.BiomeTags.SEA_WATER_SOURCE)
                || biome.unwrapKey()
                .map(key -> isFallbackSeaBiome(key.location().toString()))
                .orElse(false);
    }

    // Fraction of sea biomes in a horizontal circle of the given radius around the position.
    // Used only for the visual gradient: the bucket pickup rule remains binary.
    public static float marineFraction(Level level, BlockPos pos, int radiusBlocks) {
        if (radiusBlocks <= 0)
            return 0f;

        int marine = 0;
        int total = 0;
        int step = Math.max(1, radiusBlocks / 4);
        for (int dx = -radiusBlocks; dx <= radiusBlocks; dx += step) {
            for (int dz = -radiusBlocks; dz <= radiusBlocks; dz += step) {
                if (dx * dx + dz * dz > radiusBlocks * radiusBlocks)
                    continue;
                total++;
                if (isSeaBiome(level, pos.offset(dx, 0, dz)))
                    marine++;
            }
        }
        return total == 0 ? 0f : (float) marine / total;
    }
}
