package io.github.mortuusars.salt.gametest;

import io.github.mortuusars.salt.Evaporation;
import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import io.github.mortuusars.salt.block.SaltCauldronBlock;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Salt.ID)
@PrefixGameTestTemplate(false)
public class SaltGameTests {

    // Replicates the LivingEntity.completeUsingItem path: vanilla eating first
    // (foodData.eat from the item's food properties), then the Finish event where the mod adds the salting bonus.
    @GameTest(template = "empty")
    public static void saltedFoodGrantsBonusNutrition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0f);

        ItemStack stack = new ItemStack(Items.BAKED_POTATO);
        Salting.setSalted(stack);

        ItemStack preUseStack = stack.copy();
        ItemStack result = stack.finishUsingItem(helper.getLevel(), player);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, preUseStack, 0, result));

        // Baked potato: 5 nutrition, 6.0 saturation. Salting bonus: +2 and +2.0.
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 17, "food level");
        helper.succeed();
    }

    // Control test: regular unsalted food must restore exactly the vanilla values.
    @GameTest(template = "empty")
    public static void unsaltedFoodRestoresVanillaNutrition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0f);

        ItemStack stack = new ItemStack(Items.BAKED_POTATO);

        ItemStack preUseStack = stack.copy();
        ItemStack result = stack.finishUsingItem(helper.getLevel(), player);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, preUseStack, 0, result));

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 15, "food level");
        helper.succeed();
    }

    // The default evaporation recipe must match a water cauldron and produce the salt cauldron
    // marked as fresh-water residue, dropping sand pieces.
    @GameTest(template = "empty")
    public static void evaporationRecipeMatchesWaterCauldron(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Optional<RecipeHolder<EvaporationRecipe>> recipe =
                Evaporation.findRecipe(level, Blocks.WATER_CAULDRON.defaultBlockState());

        helper.assertTrue(recipe.isPresent(), "No evaporation recipe for water cauldron");
        helper.assertValueEqual(recipe.get().value().result().getBlock(), Salt.Blocks.SALT_CAULDRON.get(), "evaporation result");
        helper.assertValueEqual(recipe.get().value().result().getValue(SaltCauldronBlock.WATER_TYPE),
                SaltCauldronBlock.WaterType.NORMAL, "result water type");
        helper.assertValueEqual(recipe.get().value().dropsForLevel(3).getFirst().item().getItem(),
                Salt.Items.SAND_PIECE.get(), "fresh water drop");
        helper.succeed();
    }

    // Sea water must evaporate through its own recipe, producing a sea-water salt cauldron
    // that drops salt instead of sand.
    @GameTest(template = "empty")
    public static void evaporationRecipeMatchesSeaWaterCauldron(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Optional<RecipeHolder<EvaporationRecipe>> recipe = Evaporation.findRecipe(level,
                Salt.Blocks.SEA_WATER_CAULDRON.get().defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 3));

        helper.assertTrue(recipe.isPresent(), "No evaporation recipe for sea water cauldron");
        helper.assertValueEqual(recipe.get().value().result().getBlock(), Salt.Blocks.SALT_CAULDRON.get(), "evaporation result");
        helper.assertValueEqual(recipe.get().value().result().getValue(SaltCauldronBlock.WATER_TYPE),
                SaltCauldronBlock.WaterType.SEA, "result water type");
        helper.assertValueEqual(recipe.get().value().dropsForLevel(3).getFirst().item().getItem(),
                Salt.Items.SALT.get(), "sea water drop");
        helper.succeed();
    }

    // Two salt cauldrons differing only by water_type must resolve to different recipes when harvested.
    @GameTest(template = "empty")
    public static void evaporationResultLookupDistinguishesWaterTypes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockState normalResult = Salt.Blocks.SALT_CAULDRON.get().defaultBlockState()
                .setValue(SaltCauldronBlock.WATER_TYPE, SaltCauldronBlock.WaterType.NORMAL)
                .setValue(LayeredCauldronBlock.LEVEL, 3);
        BlockState seaResult = normalResult.setValue(SaltCauldronBlock.WATER_TYPE, SaltCauldronBlock.WaterType.SEA);

        Optional<RecipeHolder<EvaporationRecipe>> normalRecipe = Evaporation.findRecipeByResult(level, normalResult);
        Optional<RecipeHolder<EvaporationRecipe>> seaRecipe = Evaporation.findRecipeByResult(level, seaResult);

        helper.assertTrue(normalRecipe.isPresent(), "No recipe for fresh-water salt cauldron");
        helper.assertTrue(seaRecipe.isPresent(), "No recipe for sea-water salt cauldron");
        helper.assertValueEqual(normalRecipe.get().value().dropsForLevel(3).getFirst().item().getItem(),
                Salt.Items.SAND_PIECE.get(), "fresh-water harvest drop");
        helper.assertValueEqual(seaRecipe.get().value().dropsForLevel(3).getFirst().item().getItem(),
                Salt.Items.SALT.get(), "sea-water harvest drop");
        helper.succeed();
    }

    // The default crystal growing recipe must accept the growables-tag base and advance air into the first stage.
    @GameTest(template = "empty")
    public static void crystalGrowingRecipeAdvancesStages(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Optional<RecipeHolder<CrystalGrowingRecipe>> recipe = level.getRecipeManager()
                .getAllRecipesFor(Salt.RecipeTypes.CRYSTAL_GROWING.get()).stream().findFirst();

        helper.assertTrue(recipe.isPresent(), "No crystal growing recipes loaded");
        helper.assertTrue(recipe.get().value().matchesBaseAndStage(
                        Salt.Blocks.RAW_ROCK_SALT_BLOCK.get().defaultBlockState(), Blocks.AIR.defaultBlockState()),
                "base+air should match");
        helper.assertValueEqual(recipe.get().value().stages().get(0), Salt.Blocks.SMALL_SALT_BUD.get(), "first stage");
        helper.succeed();
    }
}
