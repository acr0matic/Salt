package io.github.mortuusars.salt.crafting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.Salting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public class SaltingRecipe extends CustomRecipe {
    private final String group;
    private final NonNullList<Ingredient> ingredients;

    public SaltingRecipe(String group, NonNullList<Ingredient> ingredients) {
        super(CraftingBookCategory.MISC);
        this.group = group;
        this.ingredients = ingredients;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        return NonNullList.withSize(container.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        boolean hasFoodInput = false;
        NonNullList<Boolean> matches = NonNullList.withSize(this.ingredients.size(), false);
        int itemsCount = 0;

        for (int slotIndex = 0; slotIndex < craftingInput.size(); slotIndex++) {
            ItemStack stackInSlot = craftingInput.getItem(slotIndex);
            if (stackInSlot.isEmpty())
                continue;

            itemsCount++;

            if (stackInSlot.is(Salt.ItemTags.CAN_BE_SALTED) && !hasFoodInput && !Salting.isSalted(stackInSlot))
                hasFoodInput = true;

            for (int ingredientIndex = 0; ingredientIndex < this.ingredients.size(); ingredientIndex++) {
                if (this.ingredients.get(ingredientIndex).test(stackInSlot) && !matches.get(ingredientIndex))
                    matches.set(ingredientIndex, true);
            }
        }

        return hasFoodInput && matches.stream().allMatch(match -> match) && itemsCount == this.ingredients.size() + 1;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider registries) {
        for (int index = 0; index < craftingInput.size(); index++) {
            ItemStack itemStack = craftingInput.getItem(index);

            if (itemStack.is(Salt.ItemTags.CAN_BE_SALTED)) {
                ItemStack resultStack = itemStack.copy();
                resultStack.setCount(1);
                return Salting.setSalted(resultStack);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 || height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Salt.RecipeSerializers.SALTING.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public Ingredient getFoodIngredient() {
        return Ingredient.of(Salt.ItemTags.CAN_BE_SALTED);
    }

    public static class Serializer implements RecipeSerializer<SaltingRecipe> {
        private static final MapCodec<SaltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
                Ingredient.LIST_CODEC_NONEMPTY.fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
        ).apply(instance, Serializer::create));

        private static final StreamCodec<RegistryFriendlyByteBuf, SaltingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, recipe -> recipe.group,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
                Serializer::create
        );

        private static SaltingRecipe create(String group, List<Ingredient> ingredients) {
            if (ingredients.size() > 9)
                throw new IllegalArgumentException("Too many ingredients for salting recipe. The maximum is 9");

            NonNullList<Ingredient> nonNullIngredients = NonNullList.create();
            nonNullIngredients.addAll(ingredients);
            return new SaltingRecipe(group, nonNullIngredients);
        }

        @Override
        public MapCodec<SaltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SaltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
