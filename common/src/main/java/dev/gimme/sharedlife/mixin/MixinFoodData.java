package dev.gimme.sharedlife.mixin;

import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MixinFoodData {

    @Shadow
    private int foodLevel;

    @Shadow
    private int lastFoodLevel;

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void tick(Player player, CallbackInfo ci) {
        if (!(player instanceof SharedLife.Heart) && player instanceof ServerPlayer serverPlayer && Players.isSharedHungerEnabled(serverPlayer)) {
            this.lastFoodLevel = this.foodLevel;
            ci.cancel();
        }
    }
}
