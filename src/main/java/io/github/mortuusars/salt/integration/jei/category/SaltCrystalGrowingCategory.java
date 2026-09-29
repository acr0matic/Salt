package io.github.mortuusars.salt.integration.jei.category;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.client.LangKeys;
import io.github.mortuusars.salt.configuration.Configuration;
import io.github.mortuusars.salt.integration.jei.SaltJeiPlugin;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SaltCrystalGrowingCategory implements IRecipeCategory<RecipeHolder<CrystalGrowingRecipe>> {
    public static final ResourceLocation UID = Salt.resource("salt_crystal_growing");
    private static final ResourceLocation TEXTURE = Salt.resource("textures/gui/jei/salt_crystal_growing.png");
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public SaltCrystalGrowingCategory(IGuiHelper guiHelper) {
        title = Salt.translate(LangKeys.JEI_CATEGORY_SALT_CRYSTAL_GROWING);
        background = guiHelper.createDrawable(TEXTURE, 0, 0, 168, 152);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Salt.Items.SALT_CLUSTER.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull RecipeHolder<CrystalGrowingRecipe> holder, @NotNull IFocusGroup focuses) {
        CrystalGrowingRecipe recipe = holder.value();

        builder.addSlot(RecipeIngredientRole.INPUT, 104, 8)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(recipe.fluid().getStacks()));

        builder.addSlot(RecipeIngredientRole.INPUT, 104, 37)
                .addItemStack(new ItemStack(Items.DRIPSTONE_BLOCK));

        builder.addSlot(RecipeIngredientRole.INPUT, 104, 66)
                .addItemStack(new ItemStack(Items.POINTED_DRIPSTONE));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 98)
                .addItemStacks(recipe.stages().stream().map(ItemStack::new).toList())
                .addRichTooltipCallback((slotView, tooltip) -> addGrowthTimeLines(tooltip, recipe));

        List<ItemStack> growables = recipe.base().stream().map(h -> new ItemStack(h.value())).filter(stack -> !stack.isEmpty()).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 104, 127)
                .addItemStacks(growables);
    }

    private static void addGrowthTimeLines(ITooltipBuilder tooltip, CrystalGrowingRecipe recipe) {
        double chance = Configuration.SALT_CLUSTER_GROWING_CHANCE.get() * recipe.chance();
        if (chance <= 0)
            return;
        int pct = (int) Math.round(chance * 100);

        // The drip chance lives in FluidType.DripstoneDripInfo.
        // For a multi-fluid ingredient we take the first one with dripInfo, so the time is approximate.
        double dripChance = 0;
        for (net.neoforged.neoforge.fluids.FluidStack stack : recipe.fluid().getStacks()) {
            var dripInfo = stack.getFluid().getFluidType().getDripInfo();
            if (dripInfo != null) {
                dripChance = dripInfo.chance();
                break;
            }
        }

        if (dripChance <= 0) {
            tooltip.add(Salt.translate(LangKeys.JEI_CATEGORY_SALT_CRYSTAL_GROWING_DRIP_CHANCE_TOOLTIP, pct)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        // A stalactite gets a random tick every ~68s, a tick produces a drip with dripChance,
        // and a drip grows a stage with chance.
        double avgSeconds = 1365.33 / (dripChance * chance) / 20;
        tooltip.add(Salt.translate(LangKeys.JEI_CATEGORY_SALT_CRYSTAL_GROWING_TIME_SECONDS_TOOLTIP,
                pct, (int) Math.round(avgSeconds)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<CrystalGrowingRecipe>> getRecipeType() {
        return SaltJeiPlugin.SALT_CRYSTAL_GROWING_RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }
    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }
    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }
}
