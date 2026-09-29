package io.github.mortuusars.salt.fluid;

import io.github.mortuusars.salt.Salt;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class SeaWaterEvents {
    private SeaWaterEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockHitResult hitResult = event.getHitVec();
        if (!shouldHandle(event.getItemStack(), level, hitResult.getBlockPos()))
            return;

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (!level.isClientSide)
            pickupSeaWater(event, hitResult.getBlockPos());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();
        HitResult hitResult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || !shouldHandle(event.getItemStack(), level, blockHitResult.getBlockPos()))
            return;

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (!level.isClientSide)
            pickupSeaWater(event, blockHitResult.getBlockPos());
    }

    private static boolean shouldHandle(ItemStack stack, Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return SeaWaterPickupRules.shouldReplaceWaterBucket(stack.is(Items.BUCKET),
                fluidState.getType() == Fluids.WATER, fluidState.isSource(), SeaWaterBiomes.isSeaBiome(level, pos));
    }

    private static void pickupSeaWater(PlayerInteractEvent event, BlockPos pos) {
        Player player = event.getEntity();
        Level level = player.level();
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);

        ItemStack filledStack = ItemUtils.createFilledResult(event.getItemStack(), player,
                new ItemStack(Salt.Items.SEA_WATER_BUCKET.get()));
        player.setItemInHand(event.getHand(), filledStack);
        player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
    }
}
