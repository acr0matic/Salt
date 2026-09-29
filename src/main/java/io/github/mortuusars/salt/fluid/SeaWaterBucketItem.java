package io.github.mortuusars.salt.fluid;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SeaWaterBucketItem extends BucketItem {
    public SeaWaterBucketItem(Fluid fluid, Item.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos,
                                 @Nullable BlockHitResult result, @Nullable ItemStack container) {
        if (SeaWaterBiomes.isSeaBiome(level, pos)) {
            return ((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, pos, result, container);
        }

        return super.emptyContents(player, level, pos, result, container);
    }
}
