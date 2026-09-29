package io.github.mortuusars.salt.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaWaterBiomesTest {
    @Test
    void recognizesMarineBiomePathFallbacks() {
        assertTrue(SeaWaterBiomes.isFallbackSeaBiome("cataclysm_dimension:ashen_ocean"));
        assertTrue(SeaWaterBiomes.isFallbackSeaBiome("spawn:deep_warm_ocean"));
        assertTrue(SeaWaterBiomes.isFallbackSeaBiome("regions_unexplored:rocky_reef"));
        assertTrue(SeaWaterBiomes.isFallbackSeaBiome("minecraft:beach"));
        assertTrue(SeaWaterBiomes.isFallbackSeaBiome("salt:inland_sea"));
    }

    @Test
    void rejectsBiomePathsWithoutMarineMarkers() {
        assertFalse(SeaWaterBiomes.isFallbackSeaBiome("minecraft:plains"));
        assertFalse(SeaWaterBiomes.isFallbackSeaBiome("minecraft:desert"));
    }

    @Test
    void replacesOnlySourceWaterFromAnEmptyBucketInSeaBiome() {
        assertTrue(SeaWaterPickupRules.shouldReplaceWaterBucket(true, true, true, true));
        assertFalse(SeaWaterPickupRules.shouldReplaceWaterBucket(false, true, true, true));
        assertFalse(SeaWaterPickupRules.shouldReplaceWaterBucket(true, false, true, true));
        assertFalse(SeaWaterPickupRules.shouldReplaceWaterBucket(true, true, false, true));
        assertFalse(SeaWaterPickupRules.shouldReplaceWaterBucket(true, true, true, false));
    }

    @Test
    void blendsVanillaWaterColorWithSeaTint() {
        assertEquals(0xFF3A78CC, SeaWaterTint.apply(0xFF3F76E4));
    }
}
