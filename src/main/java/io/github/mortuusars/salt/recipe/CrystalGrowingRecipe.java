package io.github.mortuusars.salt.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.salt.Salt;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Recipe that defines a crystal growth chain: which block it grows on ({@code base}),
 * which fluid dripping from pointed dripstone feeds it ({@code fluid}) and the
 * ordered block stages ({@code stages}) the crystal advances through:
 * air -> stages[0] -> ... -> last.
 * <pre>
 * {
 *   "type": "salt:crystal_growing",
 *   "fluid": "minecraft:water",
 *   "base": "#salt:salt_cluster_growables",
 *   "stages": ["salt:small_salt_bud", "salt:medium_salt_bud", "salt:large_salt_bud", "salt:salt_cluster"]
 * }
 * </pre>
 */
public class CrystalGrowingRecipe implements Recipe<CrystalGrowingRecipe.Input> {
    public record Input(BlockState base, BlockState current, Fluid fluid) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private final FluidIngredient fluid;
    private final HolderSet<Block> base;
    private final List<Block> stages;
    private final float chance;

    public CrystalGrowingRecipe(FluidIngredient fluid, HolderSet<Block> base, List<Block> stages, float chance) {
        this.fluid = fluid;
        this.base = base;
        this.stages = stages;
        this.chance = chance;
    }

    public FluidIngredient fluid() {
        return fluid;
    }

    public HolderSet<Block> base() {
        return base;
    }

    public List<Block> stages() {
        return stages;
    }

    /**
     * Chance multiplier applied on top of the global SaltClusterGrowingChance config value.
     */
    public float chance() {
        return chance;
    }

    /**
     * Whether the block is a stage of this chain that can still grow further.
     */
    public boolean isNonFinalStage(Block block) {
        return stages.size() > 1 && stages.subList(0, stages.size() - 1).contains(block);
    }

    /**
     * Next stage block for the given current block, or null when it is already the final stage.
     * Passing air (or any non-stage block) should be handled by the caller beforehand.
     */
    public @Nullable Block nextStageOf(Block current) {
        int index = stages.indexOf(current);
        return index < 0 || index == stages.size() - 1 ? null : stages.get(index + 1);
    }

    /**
     * Whether the crystal may occupy this spot: base matches and the position is
     * air or a non-final stage of this recipe. Ignores the dripping fluid.
     */
    public boolean matchesBaseAndStage(BlockState base, BlockState current) {
        return this.base.contains(base.getBlockHolder())
                && (current.isAir() || isNonFinalStage(current.getBlock()));
    }

    @Override
    public boolean matches(@NotNull Input input, @NotNull Level level) {
        return matchesBaseAndStage(input.base(), input.current())
                && !input.fluid().isSame(Fluids.EMPTY)
                && fluid.test(new FluidStack(input.fluid(), 1));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Input input, HolderLookup.@NotNull Provider registries) {
        return new ItemStack(stages.get(stages.size() - 1));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return new ItemStack(stages.get(stages.size() - 1));
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Salt.RecipeSerializers.CRYSTAL_GROWING.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Salt.RecipeTypes.CRYSTAL_GROWING.get();
    }

    public static class Serializer implements RecipeSerializer<CrystalGrowingRecipe> {
        private static final MapCodec<CrystalGrowingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FluidIngredient.CODEC.fieldOf("fluid").forGetter(recipe -> recipe.fluid),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("base").forGetter(recipe -> recipe.base),
                BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("stages").forGetter(recipe -> recipe.stages),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(recipe -> recipe.chance)
        ).apply(instance, CrystalGrowingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrystalGrowingRecipe> STREAM_CODEC = StreamCodec.composite(
                FluidIngredient.STREAM_CODEC, recipe -> recipe.fluid,
                ByteBufCodecs.holderSet(Registries.BLOCK), recipe -> recipe.base,
                ByteBufCodecs.registry(Registries.BLOCK).apply(ByteBufCodecs.list()), recipe -> recipe.stages,
                ByteBufCodecs.FLOAT, recipe -> recipe.chance,
                CrystalGrowingRecipe::new);

        @Override
        public @NotNull MapCodec<CrystalGrowingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CrystalGrowingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
