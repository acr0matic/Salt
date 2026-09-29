package io.github.mortuusars.salt.data.provider;

import io.github.mortuusars.salt.Salt;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTags extends ItemTagsProvider {
    public ItemTags(DataGenerator generator, CompletableFuture<HolderLookup.Provider> lookupProvider,
                    BlockTagsProvider blockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator.getPackOutput(), lookupProvider, blockTagsProvider.contentsGetter(), Salt.ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Salt.ItemTags.FORGE_TORCHES)
                .add(Items.TORCH);

        tag(Salt.ItemTags.SAND_PIECES)
                .add(Salt.Items.SAND_PIECE.get());
        tag(Salt.ItemTags.COMMON_DUSTS)
                .add(Salt.Items.SAND_PIECE.get());
        tag(Salt.ItemTags.COMMON_DUSTS_SAND)
                .add(Salt.Items.SAND_PIECE.get());

        tag(Salt.ItemTags.FORGE_SALTS)
                .add(Salt.Items.SALT.get());

        tag(Salt.ItemTags.COMMON_SALT)
                .add(Salt.Items.SALT.get());
        tag(Salt.ItemTags.COMMON_DUSTS)
                .add(Salt.Items.SALT.get());
        tag(Salt.ItemTags.COMMON_DUSTS_SALT)
                .add(Salt.Items.SALT.get());
    }
}
