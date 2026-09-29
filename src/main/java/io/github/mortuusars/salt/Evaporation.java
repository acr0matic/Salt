package io.github.mortuusars.salt;

import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Evaporation {
    private static @Nullable RecipeManager cachedManager;
    private static @Nullable RecipeManager.CachedCheck<EvaporationRecipe.Input, EvaporationRecipe> cachedCheck;

    /**
     * Finds the first evaporation recipe matching the given cauldron state.
     */
    public static Optional<RecipeHolder<EvaporationRecipe>> findRecipe(ServerLevel level, BlockState state) {
        RecipeManager manager = level.getRecipeManager();
        if (cachedCheck == null || cachedManager != manager) {
            cachedManager = manager;
            cachedCheck = RecipeManager.createCheck(Salt.RecipeTypes.EVAPORATION.get());
        }
        return cachedCheck.getRecipeFor(new EvaporationRecipe.Input(state), level);
    }

    /**
     * Finds the first evaporation recipe producing the given result block.
     * Used by result containers to determine their drops.
     */
    public static Optional<RecipeHolder<EvaporationRecipe>> findRecipeByResult(ServerLevel level, Block resultBlock) {
        return level.getRecipeManager().getAllRecipesFor(Salt.RecipeTypes.EVAPORATION.get()).stream()
                .filter(holder -> holder.value().result().getBlock() == resultBlock)
                .findFirst();
    }

    public static void onWaterCauldronAnimateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.3f)
            return;

        int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);

        float x = pos.getX() + 0.25f + random.nextFloat() * 0.5f;
        float y = pos.getY() + 0.4f + random.nextFloat() * 0.1f + waterLevel * 0.18f;
        float z = pos.getZ() + 0.25f + random.nextFloat() * 0.5f;

        level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0f, 0.08f, 0f);
        level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0f, 0.08f, 0f);

        level.playLocalSound(x, y, z,
                Salt.Sounds.BUBBLE_POP.get(), SoundSource.BLOCKS, 0.35f, 0.4f + random.nextFloat() * 0.5f, false);
    }

    /**
     * Replaces the cauldron with the evaporation recipe's result block,
     * copying over shared properties (e.g. {@code LEVEL}).
     */
    public static void evaporate(BlockState state, ServerLevel level, BlockPos pos, BlockState result, RandomSource random) {
        level.setBlockAndUpdate(pos, copySharedProperties(state, result));

        level.playSound(null, pos, Salt.Sounds.CAULDRON_EVAPORATE.get(),
                SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.2f + 0.9f);

        for (int i = 0; i < 4; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.3f + random.nextFloat() * 0.4f,
                    pos.getY() + 1f + random.nextFloat() * 0.2f,
                    pos.getZ() + 0.3f + random.nextFloat() * 0.4f,
                    1, 0, 0.02f, 0, 0.02f);
        }
    }

    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties())
            to = copyProperty(from, to, property);
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }
}
