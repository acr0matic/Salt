package io.github.mortuusars.salt.integration.jei.category;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.client.LangKeys;
import io.github.mortuusars.salt.configuration.Configuration;
import io.github.mortuusars.salt.integration.jei.SaltJeiPlugin;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class SaltEvaporationCategory implements IRecipeCategory<RecipeHolder<EvaporationRecipe>> {
    public static final ResourceLocation UID = Salt.resource("salt_evaporation");
    private static final ResourceLocation TEXTURE = Salt.resource("textures/gui/jei/salt_evaporation.png");
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    private final List<Component> heatSourceTooltip = List.of(
            Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_HEAT_SOURCE_TOOLTIP),
            Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_HEAT_SOURCE_TOOLTIP_2).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(Salt.BlockTags.HEATERS.location().toString()).withStyle(ChatFormatting.GOLD)));

    public SaltEvaporationCategory(IGuiHelper guiHelper) {
        title = Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION);
        background = guiHelper.createDrawable(TEXTURE, 0, 0, 168, 90);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Salt.Items.SALT.get()));
    }

    @Override
    public @NotNull List<Component> getTooltipStrings(@NotNull RecipeHolder<EvaporationRecipe> recipe, @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if ( (mouseX >= 15 && mouseX < 60) && (mouseY >= 50 && mouseY < 85))
            return heatSourceTooltip;

        return Collections.emptyList();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull RecipeHolder<EvaporationRecipe> holder, @NotNull IFocusGroup focuses) {
        double chance = Configuration.EVAPORATION_CHANCE.get() * holder.value().chance();
        if (chance <= 0)
            return;
        // A block gets a random tick every ~68 seconds on average (4096 blocks / 3 randomTickSpeed).
        double avgSeconds = 1365.33 / chance / 20;
        builder.addText(evaporationTimeText(avgSeconds), 70, 10)
                .setPosition(54, 14)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF404040);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull RecipeHolder<EvaporationRecipe> holder, @NotNull IFocusGroup focuses) {
        EvaporationRecipe recipe = holder.value();

        List<Fluid> inputFluids = new ArrayList<>();
        recipe.input().forEach(h -> fluidFor(h.value()).ifPresent(fluid -> {
            if (!inputFluids.contains(fluid))
                inputFluids.add(fluid);
        }));
        if (!inputFluids.isEmpty()) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 29, 14)
                    .setStandardSlotBackground();
            inputFluids.forEach(fluid -> slot.addFluidStack(fluid, 1000));
        }

        List<ItemStack> resultBlocks = List.of(new ItemStack(recipe.result().getBlock()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 121, 45).addItemStacks(resultBlocks);

        List<ItemStack> drops = new ArrayList<>();
        recipe.results().ifLeft(flat -> flat.forEach(drop -> addUniqueDrop(drops, drop)))
                .ifRight(byLevel -> byLevel.values().forEach(levelDrops ->
                        levelDrops.forEach(drop -> addUniqueDrop(drops, drop))));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 122, 14)
                .addItemStacks(List.copyOf(drops))
                .addRichTooltipCallback((slotView, tooltip) ->
                        slotView.getDisplayedItemStack().ifPresent(stack ->
                                recipe.results().ifLeft(flat ->
                                                tooltip.add(describeDrops(stack, flat).withStyle(ChatFormatting.GRAY)))
                                        .ifRight(byLevel -> {
                                            TreeMap<Integer, List<EvaporationRecipe.Drop>> sorted = new TreeMap<>(byLevel);
                                            int maxLevel = maxLevelOf(recipe, sorted);
                                            boolean headerAdded = false;
                                            for (Map.Entry<Integer, List<EvaporationRecipe.Drop>> entry : sorted.entrySet()) {
                                                boolean hasMatching = entry.getValue().stream()
                                                        .anyMatch(drop -> ItemStack.isSameItem(drop.item(), stack));
                                                if (!hasMatching)
                                                    continue;
                                                if (!headerAdded) {
                                                    tooltip.add(Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_FILLED_TOOLTIP)
                                                            .withStyle(ChatFormatting.GRAY));
                                                    headerAdded = true;
                                                }
                                                tooltip.add(Component.literal("• " + entry.getKey() + "/" + maxLevel + " — ")
                                                        .append(describeDrops(stack, entry.getValue()))
                                                        .withStyle(ChatFormatting.GRAY));
                                            }
                                        })));
    }

    /**
     * Liquid shown over the painted cauldron, marking the fluid type of the recipe input.
     */
    private static Optional<Fluid> fluidFor(Block block) {
        if (block == Blocks.WATER_CAULDRON)
            return Optional.of(Fluids.WATER);
        if (block == Salt.Blocks.SEA_WATER_CAULDRON.get())
            return Optional.of(Salt.Fluids.SEA_WATER.get());
        return Optional.empty();
    }

    private static void addUniqueDrop(List<ItemStack> drops, EvaporationRecipe.Drop drop) {
        if (drops.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, drop.item())))
            drops.add(drop.item());
    }

    private static MutableComponent evaporationTimeText(double avgSeconds) {
        return Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_TIME_SHORT_SECONDS_TOOLTIP,
                (int) Math.round(avgSeconds));
    }

    private static int maxLevelOf(EvaporationRecipe recipe, TreeMap<Integer, ?> byLevel) {
        int max = byLevel.lastKey();
        for (Holder<Block> holder : recipe.input())
            if (holder.value().defaultBlockState().hasProperty(LayeredCauldronBlock.LEVEL))
                max = Math.max(max, Collections.max(LayeredCauldronBlock.LEVEL.getPossibleValues()));
        return max;
    }

    /**
     * Builds a human-readable summary of drops for the hovered item:
     * guaranteed drops render as "100% residue" / "×N residue",
     * chance drops as "%s% chance of residue" or " + %s% extra" after guaranteed ones.
     */
    private static MutableComponent describeDrops(ItemStack hovered, List<EvaporationRecipe.Drop> drops) {
        List<EvaporationRecipe.Drop> guaranteed = new ArrayList<>();
        List<EvaporationRecipe.Drop> chanceDrops = new ArrayList<>();
        for (EvaporationRecipe.Drop drop : drops) {
            if (!ItemStack.isSameItem(drop.item(), hovered))
                continue;
            (drop.chance() >= 1f ? guaranteed : chanceDrops).add(drop);
        }

        MutableComponent line = Component.empty();
        if (!guaranteed.isEmpty()) {
            for (int i = 0; i < guaranteed.size(); i++) {
                if (i > 0)
                    line.append(", ");
                int count = guaranteed.get(i).item().getCount();
                line.append(Salt.translate(count > 1
                                ? LangKeys.JEI_CATEGORY_SALT_EVAPORATION_DROP_GUARANTEED_COUNT_TOOLTIP
                                : LangKeys.JEI_CATEGORY_SALT_EVAPORATION_DROP_GUARANTEED_TOOLTIP,
                        count));
            }
            for (EvaporationRecipe.Drop drop : chanceDrops)
                line.append(Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_DROP_CHANCE_EXTRA_TOOLTIP,
                        Math.round(drop.chance() * 100)));
        } else {
            for (int i = 0; i < chanceDrops.size(); i++) {
                if (i > 0)
                    line.append(", ");
                line.append(Salt.translate(LangKeys.JEI_CATEGORY_SALT_EVAPORATION_DROP_CHANCE_TOOLTIP,
                        Math.round(chanceDrops.get(i).chance() * 100)));
            }
        }
        return line;
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<EvaporationRecipe>> getRecipeType() {
        return SaltJeiPlugin.SALT_EVAPORATION_RECIPE_TYPE;
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
