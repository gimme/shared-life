package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the mod know when a player is saved from death by a totem of undying.
 */
@Mixin(LivingEntity.class)
public class MixinPlayerTotem {

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), require = 1)
    private void applySharedTotem(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        LivingEntity instance = (LivingEntity) (Object) this;
        if (instance instanceof ServerPlayer serverPlayer) {
            Main.INSTANCE.getPlayerHandler().onPlayerTotem(serverPlayer);
        }
    }
}
