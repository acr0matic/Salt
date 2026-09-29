package io.github.mortuusars.salt.event;

import io.github.mortuusars.salt.Melting;
import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import io.github.mortuusars.salt.integration.LegendarySurvivalOverhaulHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.jetbrains.annotations.Nullable;

public class CommonEvents {
    private static final ResourceLocation SPELUNKERY_SALT_ID =
            ResourceLocation.fromNamespaceAndPath("spelunkery", "salt");

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
            NeoForge.EVENT_BUS.addListener(CommonEvents::onRightClickBlock);
            NeoForge.EVENT_BUS.addListener(CommonEvents::onBlockDrops);
        });
    }

    // Any item from the 'c:salts' tag melts meltable blocks on right-click, not just the Salt item.
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Salt.ItemTags.FORGE_SALTS))
            return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (Melting.tryMeltFromItem(level.getBlockState(pos), pos, level)) {
            if (!event.getEntity().isCreative())
                stack.shrink(1);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            event.setCanceled(true);
            return;
        }

        // Совместимость со Spelunkery: его соль ставится патчем на землю.
        // Делегируем установку их предмету — placement считает он сам,
        // а тратится стак из контекста, т.е. предмет из c:salts.
        Item spelunkerySalt = spelunkerySaltItem();
        if (spelunkerySalt != null) {
            UseOnContext context = new UseOnContext(level, event.getEntity(), event.getHand(), stack,
                    event.getHitVec());
            InteractionResult result = spelunkerySalt.useOn(context);
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        }
    }

    // Соль Spelunkery при дропе из блоков подменяется нашей — в руках у игрока остаётся только одна соль.
    public static void onBlockDrops(BlockDropsEvent event) {
        Item spelunkerySalt = spelunkerySaltItem();
        if (spelunkerySalt == null)
            return;

        for (ItemEntity drop : event.getDrops()) {
            ItemStack dropStack = drop.getItem();
            if (dropStack.is(spelunkerySalt))
                drop.setItem(new ItemStack(Salt.Items.SALT.get(), dropStack.getCount()));
        }
    }

    private static @Nullable Item spelunkerySaltItem() {
        if (!ModList.get().isLoaded("spelunkery"))
            return null;
        return BuiltInRegistries.ITEM.getOptional(SPELUNKERY_SALT_ID).orElse(null);
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

        if (LegendarySurvivalOverhaulHandler.isLoaded()) {
            FoodProperties food = stack.getFoodProperties(player);
            if (food != null)
                LegendarySurvivalOverhaulHandler.drainHydration(player, food.nutrition());
        }
    }
}
