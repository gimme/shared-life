package dev.gimme.sharedlife.mixin;

import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies FoodData ticks to allow this mod to manage hunger.
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
}
