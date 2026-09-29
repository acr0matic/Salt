package io.github.mortuusars.salt.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SaltedFoodConsumedTrigger extends SimpleCriterionTrigger<SaltedFoodConsumedTrigger.TriggerInstance> {
    private static final Codec<TriggerInstance> CODEC = Codec.unit(new TriggerInstance());

    @Override
    public Codec<TriggerInstance> codec() {
        return CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, triggerInstance -> true);
    }

    public static final class TriggerInstance implements SimpleCriterionTrigger.SimpleInstance {
        @Override
        public Optional<net.minecraft.advancements.critereon.ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}
