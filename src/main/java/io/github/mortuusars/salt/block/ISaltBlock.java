package io.github.mortuusars.salt.block;

import io.github.mortuusars.salt.Dissolving;
import io.github.mortuusars.salt.Melting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all common functionality of salt blocks.
 */
@SuppressWarnings("unused")
public interface ISaltBlock {
    @NotNull BlockState getDissolvedState(BlockState state, ServerLevel level, BlockPos pos, Fluid fluid);

    /**
     * Performs generic salt things on random tick.
     * @return false if block was removed (by dissolving or something else)
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean onSaltRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ArrayList<Direction> directions = Arrays.stream(Direction.values())
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(directions);

        for (Direction direction : directions) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);

            if (Melting.tryMeltFromBlock(adjacentPos, level))
                break; // Melting only one block per random tick

            if (direction != Direction.DOWN && Dissolving.maybeDissolve(state, pos, adjacentState, adjacentPos, level))
                return false;
        }

        if (Dissolving.maybeDissolveInRain(getDissolvedState(state, level, pos, Fluids.WATER), level, pos))
            return false;

        return true;
    }

    default void onSaltAnimateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && level.isRainingAt(pos.above())) {
            Direction direction = Direction.getRandom(random);
            if (direction != Direction.UP) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate = level.getBlockState(blockpos);
                if (!state.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, direction.getOpposite())) {
                    double d0 = direction.getStepX() == 0 ? random.nextDouble() : 0.5D + (double)direction.getStepX() * 0.65D;
                    double d1 = direction.getStepY() == 0 ? random.nextDouble() : 0.5D + (double)direction.getStepY() * 0.65D;
                    double d2 = direction.getStepZ() == 0 ? random.nextDouble() : 0.5D + (double)direction.getStepZ() * 0.65D;
                    level.addParticle(ParticleTypes.SPIT, (double)pos.getX() + d0, (double)pos.getY() + d1, (double)pos.getZ() + d2, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}
