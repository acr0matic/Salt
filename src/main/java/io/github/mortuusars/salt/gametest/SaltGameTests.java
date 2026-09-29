package io.github.mortuusars.salt.gametest;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Salt.ID)
@PrefixGameTestTemplate(false)
public class SaltGameTests {

    // Replicates the LivingEntity.completeUsingItem path: vanilla eating first
    // (foodData.eat from the item's food properties), then the Finish event where the mod adds the salting bonus.
    @GameTest(template = "empty")
    public static void saltedFoodGrantsBonusNutrition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0f);

        ItemStack stack = new ItemStack(Items.BAKED_POTATO);
        Salting.setSalted(stack);

        ItemStack preUseStack = stack.copy();
        ItemStack result = stack.finishUsingItem(helper.getLevel(), player);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, preUseStack, 0, result));

        // Baked potato: 5 nutrition, 6.0 saturation. Salting bonus: +2 and +2.0.
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 17, "food level");
        helper.succeed();
    }

    // Control test: regular unsalted food must restore exactly the vanilla values.
    @GameTest(template = "empty")
    public static void unsaltedFoodRestoresVanillaNutrition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0f);

        ItemStack stack = new ItemStack(Items.BAKED_POTATO);

        ItemStack preUseStack = stack.copy();
        ItemStack result = stack.finishUsingItem(helper.getLevel(), player);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, preUseStack, 0, result));

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 15, "food level");
        helper.succeed();
    }
}
