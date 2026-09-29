package io.github.mortuusars.salt.data.provider;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.block.SaltCauldronBlock;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Recipes extends RecipeProvider {
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public Recipes(DataGenerator generator, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(generator.getPackOutput(), lookupProvider);
        this.lookupProvider = lookupProvider;
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        HolderGetter<Block> blocks = lookupProvider.join().lookupOrThrow(Registries.BLOCK);

        recipeOutput.accept(Salt.resource("evaporation/sand_from_water"),
                new EvaporationRecipe(
                        HolderSet.direct(BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.WATER_CAULDRON)),
                        Salt.Blocks.SALT_CAULDRON.get().defaultBlockState(),
                        Either.right(Map.of(
                                1, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SAND_PIECE.get()), 0.75f)),
                                2, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SAND_PIECE.get()), 0.9f)),
                                3, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SAND_PIECE.get()), 1f),
                                        new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SAND_PIECE.get()), 0.25f)))),
                        1f),
                null);

        recipeOutput.accept(Salt.resource("evaporation/salt_from_sea_water"),
                new EvaporationRecipe(
                        HolderSet.direct(BuiltInRegistries.BLOCK.wrapAsHolder(Salt.Blocks.SEA_WATER_CAULDRON.get())),
                        Salt.Blocks.SALT_CAULDRON.get().defaultBlockState()
                                .setValue(SaltCauldronBlock.WATER_TYPE, SaltCauldronBlock.WaterType.SEA),
                        Either.right(Map.of(
                                1, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SALT.get()), 0.75f)),
                                2, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SALT.get()), 0.9f)),
                                3, List.of(new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SALT.get()), 1f),
                                        new EvaporationRecipe.Drop(new ItemStack(Salt.Items.SALT.get()), 0.25f)))),
                        1f),
                null);

        recipeOutput.accept(Salt.resource("crystal_growing/salt_cluster"),
                new CrystalGrowingRecipe(
                        FluidIngredient.single(Fluids.WATER),
                        blocks.getOrThrow(Salt.BlockTags.SALT_CLUSTER_GROWABLES),
                        List.of(Salt.Blocks.SMALL_SALT_BUD.get(), Salt.Blocks.MEDIUM_SALT_BUD.get(),
                                Salt.Blocks.LARGE_SALT_BUD.get(), Salt.Blocks.SALT_CLUSTER.get()),
                        1f),
                null);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Salt.Items.RAW_ROCK_SALT.get(), 9)
                .requires(Salt.Items.RAW_ROCK_SALT_BLOCK.get())
                .unlockedBy("has_rock_salt", has(Salt.Items.RAW_ROCK_SALT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Salt.Items.RAW_ROCK_SALT_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', Salt.Items.RAW_ROCK_SALT.get())
                .unlockedBy("has_rock_salt", has(Salt.Items.RAW_ROCK_SALT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Salt.Items.SALT.get(), 9)
                .requires(Salt.Items.SALT_BLOCK.get())
                .unlockedBy("has_salt", has(Salt.Items.SALT.get()))
                .save(recipeOutput, Salt.resource("salt_unpacking"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Salt.Items.SALT_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', Salt.Items.SALT.get())
                .unlockedBy("has_salt", has(Salt.Items.SALT.get()))
                .save(recipeOutput, Salt.resource("salt_packing"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Salt.Items.SALT_LAMP.get())
                .pattern(" S ")
                .pattern(" T ")
                .pattern(" W ")
                .define('S', Salt.Items.RAW_ROCK_SALT_BLOCK.get())
                .define('T', Salt.ItemTags.FORGE_TORCHES)
                .define('W', ItemTags.WOODEN_SLABS)
                .unlockedBy("has_rock_salt", has(Salt.Items.RAW_ROCK_SALT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Salt.Items.SALT.get())
                .requires(Salt.Items.RAW_ROCK_SALT.get())
                .unlockedBy("has_rock_salt", has(Salt.Items.RAW_ROCK_SALT.get()))
                .save(recipeOutput, Salt.resource("salt_from_raw_rock_salt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.SAND)
                .pattern("##")
                .pattern("##")
                .define('#', Salt.ItemTags.SAND_PIECES)
                .unlockedBy("has_sand_piece", has(Salt.ItemTags.SAND_PIECES))
                .save(recipeOutput, Salt.resource("sand_from_pieces"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.GUNPOWDER, 2)
                .requires(Salt.ItemTags.FORGE_SALTS)
                .requires(Salt.ItemTags.FORGE_SALTS)
                .requires(ItemTags.COALS)
                .unlockedBy("has_salt", has(Salt.Items.SALT.get()))
                .save(recipeOutput, Salt.resource("gunpowder"));
    }
}
