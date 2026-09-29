package io.github.mortuusars.salt.data.provider;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.advancement.HarvestSaltCrystalTrigger;
import io.github.mortuusars.salt.advancement.SaltEvaporationTrigger;
import io.github.mortuusars.salt.advancement.SaltedFoodConsumedTrigger;
import io.github.mortuusars.salt.client.LangKeys;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Advancements extends AdvancementProvider {
    public Advancements(DataGenerator dataGenerator, CompletableFuture<HolderLookup.Provider> provider,
                        ExistingFileHelper existingFileHelper) {
        super(dataGenerator.getPackOutput(), provider, List.of(new SaltAdvancements(existingFileHelper)));
    }

    public static class SaltAdvancements implements AdvancementSubProvider {
        private final ExistingFileHelper existingFileHelper;

        public SaltAdvancements(ExistingFileHelper existingFileHelper) {
            this.existingFileHelper = existingFileHelper;
        }

        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> advancementConsumer) {
            AdvancementHolder tasteExplosion = Advancement.Builder.advancement()
                    .parent(ResourceLocation.parse("minecraft:husbandry/root"))
                    .display(new ItemStack(Salt.Items.SALT.get()),
                            Salt.translate(LangKeys.ADVANCEMENT_TASTE_EXPLOSION_TITLE),
                            Salt.translate(LangKeys.ADVANCEMENT_TASTE_EXPLOSION_DESCRIPTION),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("eat_salted_food", Salt.Advancements.SALTED_FOOD_CONSUMED.get()
                            .createCriterion(new SaltedFoodConsumedTrigger.TriggerInstance()))
                    .save(advancementConsumer, Salt.resource("adventure/taste_explosion"), existingFileHelper);

            AdvancementHolder boilOff = Advancement.Builder.advancement()
                    .parent(tasteExplosion)
                    .display(new ItemStack(Items.CAULDRON),
                            Salt.translate(LangKeys.ADVANCEMENT_BOIL_OFF_TITLE),
                            Salt.translate(LangKeys.ADVANCEMENT_BOIL_OFF_DESCRIPTION),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("evaporate_water_to_form_salt", Salt.Advancements.SALT_EVAPORATED.get()
                            .createCriterion(new SaltEvaporationTrigger.TriggerInstance()))
                    .save(advancementConsumer, Salt.resource("adventure/boil_off"), existingFileHelper);

            Advancement.Builder.advancement()
                    .parent(tasteExplosion)
                    .display(new ItemStack(Salt.Items.SALT_CLUSTER.get()),
                            Salt.translate(LangKeys.ADVANCEMENT_CRYSTAL_GARDEN_TITLE),
                            Salt.translate(LangKeys.ADVANCEMENT_CRYSTAL_GARDEN_DESCRIPTION),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("harvest_salt_crystal", Salt.Advancements.HARVEST_SALT_CRYSTAL.get()
                            .createCriterion(new HarvestSaltCrystalTrigger.TriggerInstance()))
                    .save(advancementConsumer, Salt.resource("adventure/crystal_garden"), existingFileHelper);
        }
    }
}
