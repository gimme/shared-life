package dev.gimme.sharedlife.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies FoodData ticks to allow this mod to manage hunger and natural regeneration.
 */
@Mixin(FoodData.class)
public abstract class MixinFoodData {

    @Shadow
    private int foodLevel;

    @Shadow
    private int lastFoodLevel;

    /**
     * Disables regular hunger mechanics for players so that it can be managed by {@link SharedLife.Heart}.
     */
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void disableHunger(Player player, CallbackInfo ci) {
        if (player instanceof SharedLife.Heart) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!Players.isSharedHungerEnabled(serverPlayer)) return;

        this.lastFoodLevel = this.foodLevel;
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
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"),
            require = 1
    )
    private boolean disableIndividualNaturalRegen(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> gameRule, Operation<Boolean> original, Player player) {
        boolean value = original.call(gameRules, gameRule);
        if (player instanceof SharedLife.Heart) return value;
        if (!(player instanceof ServerPlayer serverPlayer)) return value;
        if (!Players.isCombinedNaturalRegenerationEnabled(serverPlayer)) return value;

        return false;
    }
}
