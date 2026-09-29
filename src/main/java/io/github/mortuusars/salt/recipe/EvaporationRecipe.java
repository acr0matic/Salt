package io.github.mortuusars.salt.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.salt.Salt;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recipe that defines what a heated cauldron filled with some liquid turns into
 * and what items it drops when harvested. Driven by datapack JSONs:
 * <pre>
 * {
 *   "type": "salt:evaporation",
 *   "input": "minecraft:water_cauldron",
 *   "result": "salt:salt_cauldron",
 *   "results": {
 *     "1": [{"item": "salt:salt", "chance": 0.75}],
 *     "2": [{"item": "salt:salt", "chance": 0.9}],
 *     "3": [{"item": "salt:salt"}, {"item": "salt:salt", "chance": 0.25}]
 *   }
 * }
 * </pre>
 */
public class EvaporationRecipe implements Recipe<EvaporationRecipe.Input> {
    public record Input(BlockState state) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public record Drop(ItemStack item, float chance) {
        public static final Codec<Drop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(drop -> drop.item().getItem()),
                Codec.INT.optionalFieldOf("count", 1).forGetter(drop -> drop.item().getCount()),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(Drop::chance)
        ).apply(instance, (item, count, chance) -> new Drop(new ItemStack(item, count), chance)));

        public static final StreamCodec<RegistryFriendlyByteBuf, Drop> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Drop::item,
                ByteBufCodecs.FLOAT, Drop::chance,
                Drop::new);
    }

    private static final Codec<Map<Integer, List<Drop>>> PER_LEVEL_CODEC = Codec.unboundedMap(
            Codec.STRING.comapFlatMap(s -> {
                        try {
                            return DataResult.success(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Invalid level key: " + s);
                        }
                    },
                    String::valueOf),
            Drop.CODEC.listOf());

    public static final Codec<Either<List<Drop>, Map<Integer, List<Drop>>>> RESULTS_CODEC =
            Codec.either(Drop.CODEC.listOf(), PER_LEVEL_CODEC);

    private final HolderSet<Block> input;
    private final BlockState result;
    private final Either<List<Drop>, Map<Integer, List<Drop>>> results;
    private final float chance;

    public EvaporationRecipe(HolderSet<Block> input, BlockState result,
                             Either<List<Drop>, Map<Integer, List<Drop>>> results, float chance) {
        this.input = input;
        this.result = result;
        this.results = results;
        this.chance = chance;
    }

    public HolderSet<Block> input() {
        return input;
    }

    public BlockState result() {
        return result;
    }

    /**
     * Chance multiplier applied on top of the global EvaporationChance config value.
     */
    public float chance() {
        return chance;
    }

    public Either<List<Drop>, Map<Integer, List<Drop>>> results() {
        return results;
    }

    public List<Drop> dropsForLevel(int level) {
        return results.map(flat -> flat, byLevel -> byLevel.getOrDefault(level, List.of()));
    }

    @Override
    public boolean matches(@NotNull Input input, @NotNull Level level) {
        return this.input.contains(input.state().getBlockHolder());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Input input, HolderLookup.@NotNull Provider registries) {
        return new ItemStack(result.getBlock());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return new ItemStack(result.getBlock());
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Salt.RecipeSerializers.EVAPORATION.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Salt.RecipeTypes.EVAPORATION.get();
    }

    public static class Serializer implements RecipeSerializer<EvaporationRecipe> {
        private static final MapCodec<EvaporationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("input").forGetter(recipe -> recipe.input),
                BlockState.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                RESULTS_CODEC.fieldOf("results").forGetter(recipe -> recipe.results),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(recipe -> recipe.chance)
        ).apply(instance, EvaporationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, EvaporationRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.holderSet(Registries.BLOCK), recipe -> recipe.input,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), recipe -> recipe.result,
                ByteBufCodecs.either(
                        Drop.STREAM_CODEC.apply(ByteBufCodecs.list()),
                        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT,
                                Drop.STREAM_CODEC.apply(ByteBufCodecs.list()))),
                recipe -> recipe.results,
                ByteBufCodecs.FLOAT, recipe -> recipe.chance,
                EvaporationRecipe::new);

        @Override
        public @NotNull MapCodec<EvaporationRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, EvaporationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
