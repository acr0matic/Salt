package io.github.mortuusars.salt;

import io.github.mortuusars.salt.configuration.Configuration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class Salting {

    public record FoodValue(int nutrition, float saturationModifier) {
        @Override
        public String toString() {
            return "{Nutrition:" + nutrition + ",Saturation:" + saturationModifier + "}";
        }
    }

    private static final String SALTED_KEY = "Salted";

    public static boolean isSalted(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(SALTED_KEY);
    }

    public static ItemStack setSalted(ItemStack itemStack) {
        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> tag.putBoolean(SALTED_KEY, true));
        return itemStack;
    }

    public static FoodValue getAdditionalFoodValue(ItemStack stack) {
        FoodValue foodValue = Configuration.FOOD_VALUES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return foodValue != null ? foodValue : new FoodValue(Configuration.SALTING_ADDITIONAL_NUTRITION.get(),
                Configuration.SALTING_ADDITIONAL_SATURATION_MODIFIER.get().floatValue());
    }
}
