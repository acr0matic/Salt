package io.github.mortuusars.salt.integration;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.salt.Salting;
import io.github.mortuusars.salt.configuration.Configuration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import sfiomn.legendarysurvivaloverhaul.client.tooltips.HydrationTooltipComponent;

/**
 * Shows LSO droplet icons in the tooltip for the hydration a salted food will drain.
 * Loaded only when Legendary Survival Overhaul is present.
 */
public class LegendarySurvivalOverhaulClientHandler {
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!Salting.isSalted(stack) || !Configuration.SALTING_THIRST_ENABLED.get())
            return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        FoodProperties food = stack.getFoodProperties(player);
        if (food == null)
            return;

        int hydrationLoss = LegendarySurvivalOverhaulHandler.thirstDrainPoints(food.nutrition());
        if (hydrationLoss <= 0)
            return;

        // Отрицательное значение — LSO рисует для него отдельные "пустые" капли.
        event.getTooltipElements().add(Either.right(new HydrationTooltipComponent(-hydrationLoss, 0f)));
    }
}
