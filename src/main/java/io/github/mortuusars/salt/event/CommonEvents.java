package io.github.mortuusars.salt.event;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class CommonEvents {
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // randomTick используется миксином для превращения водяного котла в соляной.
            Blocks.WATER_CAULDRON.isRandomlyTicking = true;
            CauldronInteraction.EMPTY.map().put(Salt.Items.SEA_WATER_BUCKET.get(),
                    (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                            level, pos, player, hand, stack,
                            Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                            SoundEvents.BUCKET_EMPTY));

            Salt.registerDispenserBehaviors();
            NeoForge.EVENT_BUS.addListener(CommonEvents::onItemUseFinish);
        });
    }

    // Бонусные питательность и насыщение солёной еды докидываются поверх съеденного.
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getItem();
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof Player player)
                || !Salting.isSalted(stack))
            return;

        Salting.FoodValue additional = Salting.getAdditionalFoodValue(stack);

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(Mth.clamp(foodData.getFoodLevel() + additional.nutrition(), 0, 20));
        foodData.setSaturation(Mth.clamp(foodData.getSaturationLevel() + additional.saturation(),
                0f, foodData.getFoodLevel()));

        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative())
            Salt.Advancements.SALTED_FOOD_CONSUMED.get().trigger(serverPlayer);
    }
}
