package io.github.mortuusars.salt.integration.jei;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.configuration.Configuration;
import io.github.mortuusars.salt.crafting.recipe.SaltingRecipe;
import io.github.mortuusars.salt.integration.jei.category.SaltCrystalGrowingCategory;
import io.github.mortuusars.salt.integration.jei.category.SaltEvaporationCategory;
import io.github.mortuusars.salt.integration.jei.resource.SaltingShapelessExtension;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@JeiPlugin
public class SaltJeiPlugin implements IModPlugin {
    @SuppressWarnings("unchecked")
    public static final RecipeType<RecipeHolder<EvaporationRecipe>> SALT_EVAPORATION_RECIPE_TYPE =
            RecipeType.create(Salt.ID, "salt_evaporation", (Class<RecipeHolder<EvaporationRecipe>>) (Class<?>) RecipeHolder.class);
    @SuppressWarnings("unchecked")
    public static final RecipeType<RecipeHolder<CrystalGrowingRecipe>> SALT_CRYSTAL_GROWING_RECIPE_TYPE =
            RecipeType.create(Salt.ID, "salt_crystal_growing", (Class<RecipeHolder<CrystalGrowingRecipe>>) (Class<?>) RecipeHolder.class);

    private static final ResourceLocation ID = Salt.resource("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        if (isSaltEvaporationEnabled())
            registration.addRecipeCategories(new SaltEvaporationCategory(registration.getJeiHelpers().getGuiHelper()));

        if (isSaltCrystalGrowingEnabled())
            registration.addRecipeCategories(new SaltCrystalGrowingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (isSaltEvaporationEnabled())
            registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), SALT_EVAPORATION_RECIPE_TYPE);

        if (isSaltCrystalGrowingEnabled())
            registration.addRecipeCatalyst(new ItemStack(Salt.Items.RAW_ROCK_SALT_BLOCK.get()), SALT_CRYSTAL_GROWING_RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        RecipeManager recipeManager = getRecipeManager();
        if (recipeManager == null)
            return;

        if (isSaltEvaporationEnabled())
            registration.addRecipes(SALT_EVAPORATION_RECIPE_TYPE,
                    recipeManager.getAllRecipesFor(Salt.RecipeTypes.EVAPORATION.get()));

        if (isSaltCrystalGrowingEnabled())
            registration.addRecipes(SALT_CRYSTAL_GROWING_RECIPE_TYPE,
                    recipeManager.getAllRecipesFor(Salt.RecipeTypes.CRYSTAL_GROWING.get()));
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory()
                .addExtension(SaltingRecipe.class, new SaltingShapelessExtension());
    }

    private static @Nullable RecipeManager getRecipeManager() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getRecipeManager() : null;
    }

    private boolean isSaltEvaporationEnabled() {
        return Configuration.JEI_SALT_EVAPORATION_ENABLED.get()
                && Configuration.EVAPORATION_ENABLED.get()
                && Configuration.EVAPORATION_CHANCE.get() > 0.0d
                && BuiltInRegistries.BLOCK.getTag(Salt.BlockTags.HEATERS).map(heaters -> heaters.size() > 0).orElse(false);
    }

    private boolean isSaltCrystalGrowingEnabled() {
        return Configuration.JEI_SALT_CRYSTAL_GROWING_ENABLED.get()
                && Configuration.SALT_CLUSTER_GROWING_ENABLED.get()
                && Configuration.SALT_CLUSTER_GROWING_CHANCE.get() > 0.0d;
    }
}
