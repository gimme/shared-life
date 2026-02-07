package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the mod know when a player is healed.
 */
@Mixin(LivingEntity.class)
public class MixinPlayerHeal {

    @Inject(method = "heal", at = @At(value= "HEAD"), require = 1)
    private void applySharedHealing(float healAmount, CallbackInfo ci) {
        LivingEntity instance = (LivingEntity) (Object) this;
        if (instance instanceof ServerPlayer serverPlayer) {
            Main.INSTANCE.getPlayerHandler().onPlayerHeal(serverPlayer, healAmount);
        }
    }
}
