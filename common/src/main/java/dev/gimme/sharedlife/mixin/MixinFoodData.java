package dev.gimme.sharedlife.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies FoodData ticks to allow this mod to manage hunger and natural regeneration.
 */
@Mixin(FoodData.class)
public abstract class MixinFoodData {

    /**
     * Disables regular hunger mechanics for players so that it can be managed by {@link SharedLife.Heart}.
     */
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void disableHunger(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof SharedLife.Heart) return;
        if (!Players.isSharedHungerEnabled(player)) return;

        ci.cancel();
    }

    /**
     * Suppresses vanilla natural regeneration for individual players — by having the tick see the
     * {@code naturalRegeneration} gamerule as disabled — so that {@link SharedLife} can run one combined,
     * everyone-must-be-fed regeneration for the whole group instead. Hunger drain and starvation are left
     * untouched, and the combined regeneration still respects the real gamerule through the
     * {@link SharedLife.Heart}'s own tick.
     */
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"),
            require = 1
    )
    private Object disableIndividualNaturalRegen(GameRules gameRules, GameRule<?> gameRule, Operation<Object> original, ServerPlayer player) {
        Object value = original.call(gameRules, gameRule);
        if (player instanceof SharedLife.Heart) return value;
        if (!Players.isCombinedNaturalRegenerationEnabled(player)) return value;

        return Boolean.FALSE;
    }
}
