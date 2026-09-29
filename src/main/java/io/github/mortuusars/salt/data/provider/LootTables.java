package io.github.mortuusars.salt.data.provider;

import io.github.mortuusars.salt.Salt;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class LootTables {
    public static class BlockLoot extends VanillaBlockLoot {
        private final HolderLookup.Provider registries;

        public BlockLoot(HolderLookup.Provider registries) {
            super(registries);
            this.registries = registries;
        }

        @Override
        protected void generate() {
            dropSelf(Salt.Blocks.SALT_BLOCK.get());
            dropSelf(Salt.Blocks.RAW_ROCK_SALT_BLOCK.get());
            dropSelf(Salt.Blocks.SALT_LAMP.get());
            dropOther(Salt.Blocks.SALT_CAULDRON.get(), Blocks.CAULDRON);
            dropOther(Salt.Blocks.SEA_WATER_CAULDRON.get(), Blocks.CAULDRON);

            add(Salt.Blocks.ROCK_SALT_ORE.get(), block -> createOreDrop(block, Salt.Items.RAW_ROCK_SALT.get()));
            add(Salt.Blocks.DEEPSLATE_ROCK_SALT_ORE.get(), block -> createOreDrop(block, Salt.Items.RAW_ROCK_SALT.get()));

            dropWhenSilkTouch(Salt.Blocks.SMALL_SALT_BUD.get());
            dropWhenSilkTouch(Salt.Blocks.MEDIUM_SALT_BUD.get());
            dropWhenSilkTouch(Salt.Blocks.LARGE_SALT_BUD.get());

            Holder<Enchantment> fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
            add(Salt.Blocks.SALT_CLUSTER.get(), block ->
                    createSilkTouchDispatchTable(block, LootItem.lootTableItem(Salt.Items.RAW_ROCK_SALT.get())
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                            .apply(ApplyBonusCount.addOreBonusCount(fortune))
                            .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.CLUSTER_MAX_HARVESTABLES)))
                            .otherwise(this.applyExplosionDecay(block, LootItem.lootTableItem(Salt.Items.RAW_ROCK_SALT.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))))));
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet().stream()
                    .filter(entry -> entry.getKey().location().getNamespace().equals(Salt.ID))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }
    }
}
