package io.github.mortuusars.salt.block;

import io.github.mortuusars.salt.CrystalGrowing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SaltSandBlock extends ColoredFallingBlock implements ISaltBlock {
    private final BlockState dissolvedState;

    public SaltSandBlock(BlockState dissolvedState, int dustColor, Properties properties) {
        super(new ColorRGBA(dustColor), properties);
        this.dissolvedState = dissolvedState;
    }

    public SaltSandBlock(int dustColor, Properties properties) {
        super(new ColorRGBA(dustColor), properties);
        this.dissolvedState = Blocks.AIR.defaultBlockState();
    }

    @Override
    public @NotNull BlockState getDissolvedState(BlockState originalState, ServerLevel level, BlockPos pos, Fluid fluid) {
        return dissolvedState;
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        onSaltAnimateTick(state, level, pos, random);
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!onSaltRandomTick(state, level, pos, random))
            return;
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        CrystalGrowing.tryGrow(pos.above(), level);
    }
}
