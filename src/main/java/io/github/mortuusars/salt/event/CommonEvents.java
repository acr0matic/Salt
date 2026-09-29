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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class CommonEvents {
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // randomTick is used by the mixin to convert filled cauldrons into the evaporation result.
            Blocks.WATER_CAULDRON.isRandomlyTicking = true;
            Salt.Blocks.SEA_WATER_CAULDRON.get().isRandomlyTicking = true;
            CauldronInteraction.EMPTY.map().put(Salt.Items.SEA_WATER_BUCKET.get(),
                    (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                            level, pos, player, hand, stack,
                            Salt.Blocks.SEA_WATER_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                            SoundEvents.BUCKET_EMPTY));

            Salt.Blocks.SEA_WATER_CAULDRON_INTERACTIONS.map().put(Items.BUCKET,
                    (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                            state, level, pos, player, hand, stack,
                            new ItemStack(Salt.Items.SEA_WATER_BUCKET.get()),
                            s -> s.getValue(LayeredCauldronBlock.LEVEL) == 3,
                            SoundEvents.BUCKET_FILL));

            Salt.registerDispenserBehaviors();
            NeoForge.EVENT_BUS.addListener(CommonEvents::onItemUseFinish);
        });
    }

    // Bonus nutrition and saturation of salted food are added on top of the consumed food.
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
