package io.github.mortuusars.salt;

import io.github.mortuusars.salt.advancement.HarvestSaltCrystalTrigger;
import io.github.mortuusars.salt.advancement.SaltEvaporationTrigger;
import io.github.mortuusars.salt.advancement.SaltedFoodConsumedTrigger;
import io.github.mortuusars.salt.block.SaltBlock;
import io.github.mortuusars.salt.block.SaltCauldronBlock;
import io.github.mortuusars.salt.block.SaltClusterBlock;
import io.github.mortuusars.salt.block.SaltSandBlock;
import io.github.mortuusars.salt.configuration.Configuration;
import io.github.mortuusars.salt.crafting.recipe.SaltingRecipe;
import io.github.mortuusars.salt.recipe.CrystalGrowingRecipe;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import io.github.mortuusars.salt.event.CommonEvents;
import io.github.mortuusars.salt.fluid.SeaWaterBucketItem;
import io.github.mortuusars.salt.fluid.SeaWaterEvents;
import io.github.mortuusars.salt.item.SaltItem;
import io.github.mortuusars.salt.world.feature.MineralDepositFeature;
import io.github.mortuusars.salt.world.feature.configurations.MineralDepositConfiguration;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

@Mod(Salt.ID)
public class Salt {
    public static final String ID = "salt";

    public Salt(IEventBus modEventBus, ModContainer modContainer) {
        Configuration.init(modContainer);
        modEventBus.addListener(Configuration::onConfigLoad);
        modEventBus.addListener(Configuration::onConfigReload);

        FluidTypes.FLUID_TYPES.register(modEventBus);
        Fluids.FLUIDS.register(modEventBus);
        Blocks.BLOCKS.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        Sounds.SOUNDS.register(modEventBus);
        EntityTypes.ENTITY_TYPES.register(modEventBus);
        RecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        RecipeTypes.RECIPE_TYPES.register(modEventBus);
        WorldGenFeatures.FEATURES.register(modEventBus);
        Advancements.TRIGGERS.register(modEventBus);

        modEventBus.addListener(CommonEvents::onCommonSetup);
        NeoForge.EVENT_BUS.register(SeaWaterEvents.class);
    }

    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(Items.SALT.get(), Melting.SALT_DISPENSER_BEHAVIOR);
    }

    public static class FluidTypes {
        private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Salt.ID);

        public static final DeferredHolder<FluidType, FluidType> SEA_WATER = FLUID_TYPES.register("sea_water", () -> new FluidType(
                FluidType.Properties.create()
                        .descriptionId("fluid.salt.sea_water")
                        .canConvertToSource(false)
                        .canHydrate(false)
                        .canSwim(true)
                        .canDrown(true)
                        .canExtinguish(true)
                        .supportsBoating(true)
                        .density(1000)
                        .viscosity(1000)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    }

    public static class Fluids {
        private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Salt.ID);

        public static final DeferredHolder<Fluid, FlowingFluid> SEA_WATER = FLUIDS.register("sea_water",
                () -> new BaseFlowingFluid.Source(seaWaterProperties()));
        public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SEA_WATER = FLUIDS.register("flowing_sea_water",
                () -> new BaseFlowingFluid.Flowing(seaWaterProperties()));

        private static BaseFlowingFluid.Properties seaWaterProperties() {
            return new BaseFlowingFluid.Properties(FluidTypes.SEA_WATER,
                    () -> SEA_WATER.get(), () -> FLOWING_SEA_WATER.get())
                    .block(() -> Blocks.SEA_WATER.get())
                    .bucket(() -> Items.SEA_WATER_BUCKET.get());
        }
    }

    public static class Blocks {
        private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Salt.ID);

        public static final DeferredHolder<Block, LiquidBlock> SEA_WATER = BLOCKS.register("sea_water",
                () -> new LiquidBlock(Fluids.SEA_WATER.get(), BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER)));


        public static final DeferredHolder<Block, SaltSandBlock> SALT_BLOCK = BLOCKS.register("salt_block",
                () -> new SaltSandBlock(0xe7d5cf, BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SAND)
                        .mapColor(MapColor.COLOR_LIGHT_GRAY)
                        .randomTicks()));

        public static final DeferredHolder<Block, SaltBlock> ROCK_SALT_ORE = BLOCKS.register("rock_salt_ore",
                () -> new SaltBlock(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_GRAY)
                        .randomTicks()
                        .strength(2.5F)
                        .requiresCorrectToolForDrops()
                        .sound(Salt.Sounds.Types.SALT)));
        public static final DeferredHolder<Block, SaltBlock> DEEPSLATE_ROCK_SALT_ORE = BLOCKS.register("deepslate_rock_salt_ore",
                () -> new SaltBlock(net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ROCK_SALT_ORE.get())
                        .mapColor(MapColor.COLOR_GRAY)));

        public static final DeferredHolder<Block, SaltBlock> RAW_ROCK_SALT_BLOCK = BLOCKS.register("raw_rock_salt_block",
                () -> new SaltBlock(BlockBehaviour.Properties.ofFullCopy(ROCK_SALT_ORE.get())
                        .sound(Sounds.Types.SALT_CLUSTER)));

        public static final DeferredHolder<Block, SaltClusterBlock> SALT_CLUSTER = BLOCKS.register("salt_cluster",
                () -> new SaltClusterBlock(7, 3, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_GRAY)
                        .noOcclusion()
                        .randomTicks()
                        .strength(1.5F)
                        .sound(Salt.Sounds.Types.SALT_CLUSTER)
                        .lightLevel(state -> 3)
                        .dynamicShape()));

        public static final DeferredHolder<Block, SaltClusterBlock> LARGE_SALT_BUD = BLOCKS.register("large_salt_bud",
                () -> new SaltClusterBlock(5, 3, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                        .lightLevel(state -> 2)
                        .sound(Sounds.Types.LARGE_SALT_BUD)));

        public static final DeferredHolder<Block, SaltClusterBlock> MEDIUM_SALT_BUD = BLOCKS.register("medium_salt_bud",
                () -> new SaltClusterBlock(4, 3, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                        .lightLevel(state -> 2)
                        .sound(Sounds.Types.MEDIUM_SALT_BUD)));

        public static final DeferredHolder<Block, SaltClusterBlock> SMALL_SALT_BUD = BLOCKS.register("small_salt_bud",
                () -> new SaltClusterBlock(3, 4, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                        .lightLevel(state -> 1)
                        .sound(Sounds.Types.SMALL_SALT_BUD)));

        public static final DeferredHolder<Block, SaltCauldronBlock> SALT_CAULDRON = BLOCKS.register("salt_cauldron",
                () -> new SaltCauldronBlock(net.minecraft.world.level.biome.Biome.Precipitation.RAIN, CauldronInteraction.EMPTY));

        public static final CauldronInteraction.InteractionMap SEA_WATER_CAULDRON_INTERACTIONS =
                CauldronInteraction.newInteractionMap("sea_water");

        public static final DeferredHolder<Block, LayeredCauldronBlock> SEA_WATER_CAULDRON = BLOCKS.register("sea_water_cauldron",
                () -> new LayeredCauldronBlock(net.minecraft.world.level.biome.Biome.Precipitation.NONE, SEA_WATER_CAULDRON_INTERACTIONS,
                        BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAULDRON)));

        public static final DeferredHolder<Block, SaltBlock> SALT_LAMP = BLOCKS.register("salt_lamp",
                () -> new SaltBlock(net.minecraft.world.level.block.Blocks.SPRUCE_SLAB.defaultBlockState(),
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_ORANGE)
                                .sound(Salt.Sounds.Types.SALT)
                                .randomTicks()
                                .strength(2f)
                                .lightLevel(blockState -> 15)));
    }

    public static class Items {
        private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Salt.ID);
        public static final DeferredHolder<Item, SaltItem> SALT = ITEMS.register("salt", () -> new SaltItem(new Item.Properties()));
        public static final DeferredHolder<Item, Item> RAW_ROCK_SALT = ITEMS.register("raw_rock_salt", () -> new Item(new Item.Properties()));
        public static final DeferredHolder<Item, Item> SAND_PIECE = ITEMS.register("sand_piece", () -> new Item(new Item.Properties()));
        public static final DeferredHolder<Item, SeaWaterBucketItem> SEA_WATER_BUCKET = ITEMS.register("sea_water_bucket",
                () -> new SeaWaterBucketItem(Fluids.SEA_WATER.get(), new Item.Properties().stacksTo(1)));

        public static final DeferredHolder<Item, BlockItem> SALT_BLOCK = ITEMS.register("salt_block",
                () -> new BlockItem(Salt.Blocks.SALT_BLOCK.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> ROCK_SALT_ORE = ITEMS.register("rock_salt_ore",
                () -> new BlockItem(Salt.Blocks.ROCK_SALT_ORE.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> DEEPSLATE_ROCK_SALT_ORE = ITEMS.register("deepslate_rock_salt_ore",
                () -> new BlockItem(Salt.Blocks.DEEPSLATE_ROCK_SALT_ORE.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> RAW_ROCK_SALT_BLOCK = ITEMS.register("raw_rock_salt_block",
                () -> new BlockItem(Salt.Blocks.RAW_ROCK_SALT_BLOCK.get(), new Item.Properties()));

        public static final DeferredHolder<Item, BlockItem> SALT_CLUSTER = ITEMS.register("salt_cluster",
                () -> new BlockItem(Salt.Blocks.SALT_CLUSTER.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> LARGE_SALT_BUD = ITEMS.register("large_salt_bud",
                () -> new BlockItem(Salt.Blocks.LARGE_SALT_BUD.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> MEDIUM_SALT_BUD = ITEMS.register("medium_salt_bud",
                () -> new BlockItem(Salt.Blocks.MEDIUM_SALT_BUD.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> SMALL_SALT_BUD = ITEMS.register("small_salt_bud",
                () -> new BlockItem(Salt.Blocks.SMALL_SALT_BUD.get(), new Item.Properties()));
        public static final DeferredHolder<Item, BlockItem> SALT_LAMP = ITEMS.register("salt_lamp",
                () -> new BlockItem(Salt.Blocks.SALT_LAMP.get(), new Item.Properties()));
    }

    public static class Advancements {
        private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Salt.ID);
        public static final DeferredHolder<CriterionTrigger<?>, SaltEvaporationTrigger> SALT_EVAPORATED =
                TRIGGERS.register("salt_evaporated", SaltEvaporationTrigger::new);
        public static final DeferredHolder<CriterionTrigger<?>, SaltedFoodConsumedTrigger> SALTED_FOOD_CONSUMED =
                TRIGGERS.register("salted_food_consumed", SaltedFoodConsumedTrigger::new);
        public static final DeferredHolder<CriterionTrigger<?>, HarvestSaltCrystalTrigger> HARVEST_SALT_CRYSTAL =
                TRIGGERS.register("harvest_salt_crystal", HarvestSaltCrystalTrigger::new);
    }

    public static class Sounds {
        private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Salt.ID);

        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_DISSOLVE = registerBlockSound("salt.dissolve");
        public static final DeferredHolder<SoundEvent, SoundEvent> MELT = registerBlockSound("melt");
        public static final DeferredHolder<SoundEvent, SoundEvent> CAULDRON_EVAPORATE = registerBlockSound("cauldron.evaporate");
        public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_POP = registerBlockSound("bubble_pop");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CAULDRON_REMOVE_SALT = registerBlockSound("salt_cauldron.remove_salt");

        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_BREAK = registerBlockSound("salt.break");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_STEP = registerBlockSound("salt.step");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_PLACE = registerBlockSound("salt.place");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_HIT = registerBlockSound("salt.hit");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_FALL = registerBlockSound("salt.fall");

        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_BREAK = registerBlockSound("salt_cluster.break");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_STEP = registerBlockSound("salt_cluster.step");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_PLACE = registerBlockSound("salt_cluster.place");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_HIT = registerBlockSound("salt_cluster.hit");
        public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_FALL = registerBlockSound("salt_cluster.fall");

        public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_BREAK = registerBlockSound("large_salt_bud.break");
        public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_STEP = registerBlockSound("large_salt_bud.step");
        public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_PLACE = registerBlockSound("large_salt_bud.place");
        public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_HIT = registerBlockSound("large_salt_bud.hit");
        public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_FALL = registerBlockSound("large_salt_bud.fall");

        public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_BREAK = registerBlockSound("medium_salt_bud.break");
        public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_STEP = registerBlockSound("medium_salt_bud.step");
        public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_PLACE = registerBlockSound("medium_salt_bud.place");
        public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_HIT = registerBlockSound("medium_salt_bud.hit");
        public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_FALL = registerBlockSound("medium_salt_bud.fall");

        public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_BREAK = registerBlockSound("small_salt_bud.break");
        public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_STEP = registerBlockSound("small_salt_bud.step");
        public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_PLACE = registerBlockSound("small_salt_bud.place");
        public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_HIT = registerBlockSound("small_salt_bud.hit");
        public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_FALL = registerBlockSound("small_salt_bud.fall");

        private static DeferredHolder<SoundEvent, SoundEvent> registerBlockSound(String name) {
            return SOUNDS.register("block.salt." + name,
                    () -> SoundEvent.createVariableRangeEvent(Salt.resource("block.salt." + name)));
        }

        public static class Types {
            public static final SoundType SALT = new DeferredSoundType(1.0f, 1.0f, SALT_BREAK, SALT_STEP, SALT_PLACE, SALT_HIT, SALT_FALL);
            public static final SoundType SALT_CLUSTER = new DeferredSoundType(1.0f, 1.0f, SALT_CLUSTER_BREAK, SALT_CLUSTER_STEP, SALT_CLUSTER_PLACE, SALT_CLUSTER_HIT, SALT_CLUSTER_FALL);
            public static final SoundType LARGE_SALT_BUD = new DeferredSoundType(1.0f, 1.0f, LARGE_SALT_BUD_BREAK, LARGE_SALT_BUD_STEP, LARGE_SALT_BUD_PLACE, LARGE_SALT_BUD_HIT, LARGE_SALT_BUD_FALL);
            public static final SoundType MEDIUM_SALT_BUD = new DeferredSoundType(1.0f, 1.0f, MEDIUM_SALT_BUD_BREAK, MEDIUM_SALT_BUD_STEP, MEDIUM_SALT_BUD_PLACE, MEDIUM_SALT_BUD_HIT, MEDIUM_SALT_BUD_FALL);
            public static final SoundType SMALL_SALT_BUD = new DeferredSoundType(1.0f, 1.0f, SMALL_SALT_BUD_BREAK, SMALL_SALT_BUD_STEP, SMALL_SALT_BUD_PLACE, SMALL_SALT_BUD_HIT, SMALL_SALT_BUD_FALL);
        }
    }

    public static class ItemTags {
        public static final TagKey<Item> CAN_BE_SALTED = TagKey.create(Registries.ITEM, Salt.resource("can_be_salted"));
        public static final TagKey<Item> SAND_PIECES = TagKey.create(Registries.ITEM, Salt.resource("sand_pieces"));
        public static final TagKey<Item> FORGE_SALTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "salts"));
        public static final TagKey<Item> FORGE_TORCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "torches"));
        public static final TagKey<Item> COMMON_SALT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "salt"));
        public static final TagKey<Item> COMMON_DUSTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts"));
        public static final TagKey<Item> COMMON_DUSTS_SALT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/salt"));
        public static final TagKey<Item> COMMON_DUSTS_SAND = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/sand"));
    }

    public static class BlockTags {
        public static final TagKey<Block> HEATERS = TagKey.create(Registries.BLOCK, Salt.resource("heaters"));
        public static final TagKey<Block> SALT_CLUSTER_GROWABLES = TagKey.create(Registries.BLOCK, Salt.resource("salt_cluster_growables"));
        public static final TagKey<Block> SALT_DISSOLVABLES = TagKey.create(Registries.BLOCK, Salt.resource("salt_dissolvables"));
        public static final TagKey<Block> MELTABLES = TagKey.create(Registries.BLOCK, Salt.resource("meltables"));
        public static final TagKey<Block> SALT_CLUSTER_REPLACEABLES = TagKey.create(Registries.BLOCK, Salt.resource("salt_cluster_replaceables"));
    }

    public static class BiomeTags {
        public static final TagKey<Biome> HAS_ROCK_SALT_DEPOSITS = TagKey.create(Registries.BIOME, Salt.resource("has_rock_salt_deposits"));
        public static final TagKey<Biome> SEA_WATER_SOURCE = TagKey.create(Registries.BIOME, Salt.resource("sea_water_source"));
    }

    public static class EntityTypes {
        private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Salt.ID);
    }

    public static class RecipeSerializers {
        private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Salt.ID);
        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SaltingRecipe>> SALTING =
                RECIPE_SERIALIZERS.register("salting", SaltingRecipe.Serializer::new);
        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EvaporationRecipe>> EVAPORATION =
                RECIPE_SERIALIZERS.register("evaporation", EvaporationRecipe.Serializer::new);
        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrystalGrowingRecipe>> CRYSTAL_GROWING =
                RECIPE_SERIALIZERS.register("crystal_growing", CrystalGrowingRecipe.Serializer::new);
    }

    public static class RecipeTypes {
        private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Salt.ID);
        public static final DeferredHolder<RecipeType<?>, RecipeType<EvaporationRecipe>> EVAPORATION =
                RECIPE_TYPES.register("evaporation", () -> RecipeType.simple(Salt.resource("evaporation")));
        public static final DeferredHolder<RecipeType<?>, RecipeType<CrystalGrowingRecipe>> CRYSTAL_GROWING =
                RECIPE_TYPES.register("crystal_growing", () -> RecipeType.simple(Salt.resource("crystal_growing")));
    }

    public static class WorldGenFeatures {
        private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, Salt.ID);
        public static final DeferredHolder<Feature<?>, MineralDepositFeature> MINERAL_DEPOSIT = FEATURES.register("mineral_deposit",
                () -> new MineralDepositFeature(MineralDepositConfiguration.CODEC));
    }

    public static class PlacedFeatures {
        public static final ResourceKey<PlacedFeature> MINERAL_ROCK_SALT_PLACED_KEY = createKey("mineral_rock_salt");

        public static void bootstrap(BootstrapContext<PlacedFeature> context) {
            HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
            register(context, MINERAL_ROCK_SALT_PLACED_KEY, configuredFeatures.getOrThrow(ConfiguredFeatures.MINERAL_ROCK_SALT),
                    CountPlacement.of(1), RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(-5), VerticalAnchor.absolute(110)), BiomeFilter.biome());
        }

        private static ResourceKey<PlacedFeature> createKey(String name) {
            return ResourceKey.create(Registries.PLACED_FEATURE, Salt.resource(name));
        }

        private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                     Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
            context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
        }

        private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                     Holder<ConfiguredFeature<?, ?>> configuration, PlacementModifier... modifiers) {
            register(context, key, configuration, List.of(modifiers));
        }
    }

    public static class ConfiguredFeatures {
        public static final ResourceKey<ConfiguredFeature<?, ?>> MINERAL_ROCK_SALT = registerKey("mineral_rock_salt");

        private static final Supplier<MineralDepositConfiguration.DepositBlockStateInfo> ROCK_SALT_STONE_BLOCKS = () -> MineralDepositConfiguration.blockStateInfo(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.ROCK_SALT_ORE.get().defaultBlockState(), 8)
                        .add(Blocks.RAW_ROCK_SALT_BLOCK.get().defaultBlockState(), 1)
                        .build()), new TagMatchTest(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES));

        private static final Supplier<MineralDepositConfiguration.DepositBlockStateInfo> ROCK_SALT_DEEPSLATE_BLOCKS = () -> MineralDepositConfiguration.blockStateInfo(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.DEEPSLATE_ROCK_SALT_ORE.get().defaultBlockState(), 8)
                        .add(Blocks.RAW_ROCK_SALT_BLOCK.get().defaultBlockState(), 1)
                        .build()), new TagMatchTest(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES));

        private static final Supplier<MineralDepositConfiguration.DepositBlockStateInfo> ROCK_SALT_CLUSTERS = () -> MineralDepositConfiguration.blockStateInfo(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SALT_CLUSTER.get().defaultBlockState(), 16)
                        .add(Blocks.LARGE_SALT_BUD.get().defaultBlockState(), 2)
                        .add(Blocks.MEDIUM_SALT_BUD.get().defaultBlockState(), 1)
                        .add(Blocks.SMALL_SALT_BUD.get().defaultBlockState(), 1)
                        .build()), new TagMatchTest(BlockTags.SALT_CLUSTER_REPLACEABLES));

        public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
            register(context, MINERAL_ROCK_SALT, WorldGenFeatures.MINERAL_DEPOSIT.get(),
                    new MineralDepositConfiguration(List.of(ROCK_SALT_STONE_BLOCKS.get(), ROCK_SALT_DEEPSLATE_BLOCKS.get()), ROCK_SALT_CLUSTERS.get()));
        }

        public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE, Salt.resource(name));
        }

        private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
                BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
            context.register(key, new ConfiguredFeature<>(feature, configuration));
        }
    }
}
