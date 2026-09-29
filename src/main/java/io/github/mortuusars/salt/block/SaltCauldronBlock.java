package io.github.mortuusars.salt.block;

import io.github.mortuusars.salt.Evaporation;
import io.github.mortuusars.salt.Salt;
import io.github.mortuusars.salt.helper.Heater;
import io.github.mortuusars.salt.recipe.EvaporationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;


public class SaltCauldronBlock extends LayeredCauldronBlock {
    public static final EnumProperty<WaterType> WATER_TYPE = EnumProperty.create("water_type", WaterType.class);

    private final Biome.Precipitation precipitationType;

    public SaltCauldronBlock(Biome.Precipitation precipitationType, CauldronInteraction.InteractionMap interactions) {
        super(precipitationType, interactions, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));
        this.precipitationType = precipitationType;
        this.registerDefaultState(this.defaultBlockState().setValue(WATER_TYPE, WaterType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATER_TYPE);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    public void destroy(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
        super.destroy(level, pos, state);
        if (level instanceof ServerLevel serverLevel)
            dropContents(serverLevel, state, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                               Player player, @NotNull BlockHitResult blockHitResult) {
        if (level instanceof ServerLevel serverLevel) {
            dropContents(serverLevel, state, pos);
            serverLevel.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            serverLevel.playSound(null, pos, Salt.Sounds.SALT_CAULDRON_REMOVE_SALT.get(), SoundSource.BLOCKS,
                    0.8F, serverLevel.getRandom().nextFloat() * 0.2f + 0.9f);
            if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative())
                Salt.Advancements.SALT_EVAPORATED.get().trigger(serverPlayer);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    protected void dropContents(ServerLevel level, BlockState state, BlockPos pos) {
        int fullness = state.getValue(LEVEL);
        Evaporation.findRecipeByResult(level, state).ifPresent(holder -> {
            for (EvaporationRecipe.Drop drop : holder.value().dropsForLevel(fullness)) {
                if (level.random.nextFloat() <= drop.chance())
                    spawnDrop(level, pos, drop.item().copy());
            }
        });
    }

    private void spawnDrop(ServerLevel level, BlockPos pos, ItemStack itemStack) {
        float x = pos.getX() + 0.4f + level.random.nextFloat() * 0.2f;
        float y = pos.getY() + 0.7f + level.random.nextFloat() * 0.2f;
        float z = pos.getZ() + 0.4f + level.random.nextFloat() * 0.2f;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
        itemEntity.setPickUpDelay(5);
        level.addFreshEntity(itemEntity);
    }

    @Override
    public void entityInside(@NotNull BlockState state, Level level, BlockPos pos, @NotNull Entity entity) {
        if (Heater.isHeatSource(level.getBlockState(pos.below()))) {
            if (!entity.fireImmune() && entity instanceof LivingEntity livingEntity && !hasFrostWalker(livingEntity))
                entity.hurt(level.damageSources().onFire(), 1f);
        }
    }

    private static boolean hasFrostWalker(LivingEntity entity) {
        RegistryAccess registryAccess = entity.registryAccess();
        Holder<Enchantment> frostWalker = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FROST_WALKER);
        return EnchantmentHelper.getEnchantmentLevel(frostWalker, entity) > 0;
    }

    @Override
    public void handlePrecipitation(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                    Biome.@NotNull Precipitation precipitation) {
        if (CauldronBlock.shouldHandlePrecipitation(level, precipitation)
                && Heater.isHeatSource(level.getBlockState(pos.below())) && precipitation == this.precipitationType) {
            if (precipitation == Biome.Precipitation.RAIN) {
                level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            } else if (precipitation == Biome.Precipitation.SNOW) {
                level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
        }
    }

    @Override
    protected boolean canReceiveStalactiteDrip(@NotNull Fluid fluid) {
        return true;
    }

    @Override
    protected void receiveStalactiteDrip(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Fluid fluid) {
        if (fluid == Fluids.LAVA) {
            level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
            level.levelEvent(LevelEvent.SOUND_DRIP_LAVA_INTO_CAULDRON, pos, 0);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        } else if (Heater.isHeatSource(level.getBlockState(pos.below()))) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.5f, level.getRandom().nextFloat() * 0.2f + 0.9f);
        } else if (fluid == Fluids.WATER) {
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
            level.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
    }

    public enum WaterType implements net.minecraft.util.StringRepresentable {
        NORMAL("normal"),
        SEA("sea");

        private final String name;

        WaterType(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
