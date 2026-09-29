package io.github.mortuusars.salt.integration;

import io.github.mortuusars.salt.configuration.Configuration;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.util.AttachmentUtil;

/**
 * Drains hydration when salted food is eaten. The drain scales with the food's
 * base nutrition and is clamped to configured droplet bounds.
 */
public class LegendarySurvivalOverhaulHandler {
    public static final String MOD_ID = "legendarysurvivaloverhaul";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * @param baseNutrition nutrition of the food without the salting bonus
     * @return hydration points (half-droplets) the food would drain, rounded down
     */
    public static int thirstDrainPoints(int baseNutrition) {
        double drops = Mth.clamp(baseNutrition * Configuration.SALTING_THIRST_MULTIPLIER.get(),
                Configuration.SALTING_THIRST_MIN.get(), Configuration.SALTING_THIRST_MAX.get());
        // 1 droplet = 2 hydration points; floor to the nearest half-droplet.
        return Math.max(0, (int) Math.floor(drops * 2 + 1e-9));
    }

    /**
     * @param baseNutrition nutrition of the food without the salting bonus
     */
    public static void drainHydration(Player player, int baseNutrition) {
        if (!Configuration.SALTING_THIRST_ENABLED.get() || !ThirstUtil.isThirstActive(player))
            return;

        int hydrationLoss = thirstDrainPoints(baseNutrition);
        if (hydrationLoss <= 0)
            return;

        // ThirstUtil.takeDrink() пропускает добавление значения при полной шкале
        // жажды — снятие через него в таком случае теряется, поэтому работаем
        // с attachment напрямую (addHydrationLevel клампит результат в 0..20).
        AttachmentUtil.getThirstAttachment(player).addHydrationLevel(-hydrationLoss);
    }
}
