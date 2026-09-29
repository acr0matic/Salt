package io.github.mortuusars.salt.event;

import io.github.mortuusars.salt.Salt;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

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
        });
    }
}
