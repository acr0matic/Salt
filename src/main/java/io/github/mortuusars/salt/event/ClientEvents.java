package io.github.mortuusars.salt.event;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import io.github.mortuusars.salt.client.LangKeys;
import io.github.mortuusars.salt.client.rendering.LayeredBakedModel;
import io.github.mortuusars.salt.integration.AppleSkinHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Salt.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void creativeTabEvent(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(Salt.Items.SALT.get());
            event.accept(Salt.Items.RAW_ROCK_SALT.get());
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(Salt.Items.SALT_BLOCK.get());
            event.accept(Salt.Items.ROCK_SALT_ORE.get());
            event.accept(Salt.Items.DEEPSLATE_ROCK_SALT_ORE.get());
            event.accept(Salt.Items.RAW_ROCK_SALT_BLOCK.get());
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(Salt.Items.SMALL_SALT_BUD.get());
            event.accept(Salt.Items.MEDIUM_SALT_BUD.get());
            event.accept(Salt.Items.LARGE_SALT_BUD.get());
            event.accept(Salt.Items.SALT_CLUSTER.get());
            event.accept(Salt.Items.SALT_LAMP.get());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent ignoredEvent) {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltipEvent);
        if (ModList.get().isLoaded("appleskin"))
            NeoForge.EVENT_BUS.register(new AppleSkinHandler());
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(Salt.resource("item/salted_overlay")));
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted ignoredEvent) {
        LayeredBakedModel.Cache.clear();
    }

    public static void onItemTooltipEvent(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (Salting.isSalted(itemStack)) {
            List<Component> toolTip = event.getToolTip();
            Salting.FoodValue additionalFoodValue = Salting.getAdditionalFoodValue(itemStack);
            toolTip.add(toolTip.size() >= 1 ? 1 : 0, SaltedTooltip.get(additionalFoodValue.nutrition(),
                    additionalFoodValue.saturationModifier(), Screen.hasShiftDown() && !ModList.get().isLoaded("appleskin")));
        }
    }

    public static class SaltedTooltip {
        public static final Style SALTED_STYLE = Style.EMPTY.withColor(0xF0D8D5);
        public static final Style SALTED_EXPANDED_PART_STYLE = Style.EMPTY.withColor(0xC7B7B5);

        public static MutableComponent get(int nutrition, float saturationModifier, boolean isExpanded) {
            MutableComponent base = Component.translatable(LangKeys.GUI_TOOLTIP_SALTED).withStyle(SALTED_STYLE);
            return isExpanded ? base.append(Component.translatable(LangKeys.GUI_TOOLTIP_SALTED_EXPANDED_PART,
                    nutrition > 0 ? "+" + nutrition : "-" + nutrition,
                    saturationModifier > 0 ? "+" + saturationModifier : "-" + saturationModifier)
                    .withStyle(SALTED_EXPANDED_PART_STYLE))
                    : base;
        }
    }
}
