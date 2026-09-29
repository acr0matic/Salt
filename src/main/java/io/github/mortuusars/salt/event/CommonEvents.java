package io.github.mortuusars.salt.event;

import io.github.mortuusars.salt.Salt;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonEvents {
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // randomTick используется миксином для превращения водяного котла в соляной.
            Blocks.WATER_CAULDRON.isRandomlyTicking = true;

            Salt.registerDispenserBehaviors();
        });
    }
}
