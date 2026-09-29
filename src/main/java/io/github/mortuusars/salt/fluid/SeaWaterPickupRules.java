package io.github.mortuusars.salt.fluid;

public final class SeaWaterPickupRules {
    private SeaWaterPickupRules() {
    }

    public static boolean shouldReplaceWaterBucket(boolean isEmptyBucket, boolean isWater,
                                                   boolean isSource, boolean isSeaBiome) {
        return isEmptyBucket && isWater && isSource && isSeaBiome;
    }
}
