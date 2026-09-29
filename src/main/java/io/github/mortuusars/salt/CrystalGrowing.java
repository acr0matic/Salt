package io.github.mortuusars.salt;

import io.github.mortuusars.salt.block.SaltClusterBlock;
import io.github.mortuusars.salt.configuration.Configuration;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/**
 * Handles crystal cluster growth driven by {@code salt:crystal_growing} recipes:
 * a fluid dripping from pointed dripstone onto a matching base block grows the
 * next stage of the recipe's chain above it.
 */
public class CrystalGrowing {
    public static Fluid getFluidDrippingOn(ServerLevel level, BlockPos pos) {
        BlockPos blockpos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        return blockpos == null ? Fluids.EMPTY : PointedDripstoneBlock.getCauldronFillFluidType(level, blockpos);
    }

    /**
     * Whether any crystal growing recipe can grow on this base block.
     */
    public static boolean isGrowableBase(ServerLevel level, BlockState baseState) {
        return level.getRecipeManager().getAllRecipesFor(Salt.RecipeTypes.CRYSTAL_GROWING.get()).stream()
                .anyMatch(holder -> holder.value().base().contains(baseState.getBlockHolder()));
    }

    /**
     * Whether the block is a valid drip target for crystal growth:
     * a growable base or a non-final stage block facing UP.
     */
    public static boolean isGrowableTarget(ServerLevel level, BlockState state) {
        for (RecipeHolder<CrystalGrowingRecipe> holder :
                level.getRecipeManager().getAllRecipesFor(Salt.RecipeTypes.CRYSTAL_GROWING.get())) {
            CrystalGrowingRecipe recipe = holder.value();
            if (recipe.base().contains(state.getBlockHolder()))
                return true;

            if (recipe.isNonFinalStage(state.getBlock())
                    && state.hasProperty(SaltClusterBlock.FACING)
                    && state.getValue(SaltClusterBlock.FACING) == Direction.UP)
                return true;
        }
        return false;
    }

    /**
     * Finds the first recipe matching base + current position state (dripping fluid ignored).
     */
    public static Optional<RecipeHolder<CrystalGrowingRecipe>> findRecipe(ServerLevel level, BlockState baseState, BlockState currentState) {
        return level.getRecipeManager().getAllRecipesFor(Salt.RecipeTypes.CRYSTAL_GROWING.get()).stream()
                .filter(holder -> holder.value().matchesBaseAndStage(baseState, currentState))
                .findFirst();
    }

    /**
     * Handles Cluster 'life-cycle' at the position above a growable base.
     * Grows the next stage when the dripping fluid matches the recipe,
     * destroys the cluster when a foreign fluid drips on it.
     */
    public static void tryGrow(BlockPos clusterPos, ServerLevel level) {
        if (!Configuration.SALT_CLUSTER_GROWING_ENABLED.get())
            return;

        BlockState baseState = level.getBlockState(clusterPos.below());
        BlockState clusterState = level.getBlockState(clusterPos);
        Optional<RecipeHolder<CrystalGrowingRecipe>> recipe = findRecipe(level, baseState, clusterState);
        if (recipe.isEmpty())
            return;

        Fluid drippingFluid = getFluidDrippingOn(level, clusterPos);
        if (drippingFluid == Fluids.EMPTY)
            return;

        CrystalGrowingRecipe value = recipe.get().value();
        if (value.fluid().test(new FluidStack(drippingFluid, 1))) {
            if (level.random.nextFloat() < Configuration.SALT_CLUSTER_GROWING_CHANCE.get() * value.chance())
                grow(level, clusterPos, value);
        } else {
            level.destroyBlock(clusterPos, false);
        }
    }

    public static void grow(ServerLevel level, BlockPos clusterPos, CrystalGrowingRecipe recipe) {
        BlockState prevState = level.getBlockState(clusterPos);
        var next = prevState.isAir() ? recipe.stages().get(0) : recipe.nextStageOf(prevState.getBlock());
        if (next == null)
            return;

        BlockState clusterState = next.defaultBlockState();
        if (clusterState.hasProperty(SaltClusterBlock.FACING))
            clusterState = clusterState.setValue(SaltClusterBlock.FACING, Direction.UP);

        level.setBlockAndUpdate(clusterPos, clusterState);

        BlockState placedClusterState = level.getBlockState(clusterPos);
        level.playSound(null, clusterPos, placedClusterState.getBlock().getSoundType(placedClusterState, level, clusterPos, null).getPlaceSound(),
                SoundSource.BLOCKS, 0.3f, level.random.nextFloat() * 0.3f + 0.75f);

        BlockParticleOption particleType = new BlockParticleOption(ParticleTypes.BLOCK, placedClusterState);
        VoxelShape shape = placedClusterState.getShape(level, clusterPos);
        for (int i = 0; i < 8; i++) {
            level.sendParticles(particleType,
                    clusterPos.getX() + level.random.triangle(shape.min(Direction.Axis.X), shape.max(Direction.Axis.X)),
                    clusterPos.getY() + level.random.triangle(shape.min(Direction.Axis.Y), shape.max(Direction.Axis.Y)),
                    clusterPos.getZ() + level.random.triangle(shape.min(Direction.Axis.Z), shape.max(Direction.Axis.Z)),
                    1, 0f, 0f, 0f, 0f);
        }
    }
}
