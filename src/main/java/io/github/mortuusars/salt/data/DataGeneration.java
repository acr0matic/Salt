package io.github.mortuusars.salt.data;

import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.data.provider.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Salt.ID)
public class DataGeneration
{
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new Advancements(generator, lookupProvider, helper));
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(LootTables.BlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(LootTables.GameplayLoot::new, LootContextParamSets.EMPTY)),
                lookupProvider));
        generator.addProvider(event.includeServer(), new Recipes(generator, lookupProvider));
        BlockTags blockTags = new BlockTags(generator, lookupProvider, helper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTags(generator, lookupProvider, blockTags, helper));
        generator.addProvider(event.includeServer(), new BiomeTags(generator, lookupProvider, helper));
        generator.addProvider(event.includeServer(), new WorldGen(packOutput, lookupProvider));


        BlockStatesAndModels blockStates = new BlockStatesAndModels(generator, helper);
        generator.addProvider(event.includeClient(), blockStates);
        generator.addProvider(event.includeClient(), new ItemModels(generator, blockStates.models().existingFileHelper));
        generator.addProvider(event.includeClient(), new Sounds(generator, helper));
    }
}