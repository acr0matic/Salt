package io.github.mortuusars.salt.mixin;

import io.github.mortuusars.salt.fluid.SeaWaterBiomes;
import io.github.mortuusars.salt.fluid.SeaWaterTint;
import io.github.mortuusars.salt.fluid.SeaWaterVisuals;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Redirect(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;getTintColor(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int salt$modifyWaterTint(IClientFluidTypeExtensions extensions, FluidState state,
                                     BlockAndTintGetter getter, BlockPos pos) {
        int vanillaColor = extensions.getTintColor(state, getter, pos);
        Level level = getter instanceof Level vanillaLevel ? vanillaLevel
                : getter instanceof RenderChunkRegionAccessor region ? region.salt$getLevel() : null;
        if (state.getType() != Fluids.WATER || level == null)
            return vanillaColor;

        int seaWaterColor = SeaWaterVisuals.seaWaterColor();
        int strength = SeaWaterVisuals.marineBlendPercent();
        if (SeaWaterBiomes.isSeaBiome(level, pos))
            return SeaWaterTint.apply(vanillaColor, seaWaterColor, strength);

        // Пресная вода: тинт затухает наружу от морской границы по доле морских биомов в радиусе.
        float fraction = SeaWaterBiomes.marineFraction(level, pos, SeaWaterVisuals.marineTintRadius());
        int percent = Math.round(strength * fraction);
        return percent > 0 ? SeaWaterTint.apply(vanillaColor, seaWaterColor, percent) : vanillaColor;
    }
}
