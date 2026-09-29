package io.github.mortuusars.salt.integration;

import io.github.mortuusars.salt.Salting;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;

public class AppleSkinHandler {
    @SubscribeEvent
    public void onFoodValuesGetEvent(FoodValuesEvent event) {
        ItemStack foodStack = event.itemStack;
        if (!Salting.isSalted(foodStack))
            return;

        Salting.FoodValue additionalFoodValue = Salting.getAdditionalFoodValue(foodStack);
        FoodProperties foodProperties = event.modifiedFoodProperties;
        event.modifiedFoodProperties = new FoodProperties.Builder()
                .nutrition(foodProperties.nutrition() + additionalFoodValue.nutrition())
                .saturationModifier(foodProperties.saturation() + additionalFoodValue.saturationModifier())
                .build();
    }
}
