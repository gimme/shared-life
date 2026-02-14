package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the mod know when a player dies.
 */
@Mixin(ServerPlayer.class)
public class MixinPlayerDie {

    @Inject(method = "die", at = @At(value= "HEAD"), require = 1)
    private void applySharedDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer instance = (ServerPlayer) (Object) this;
        Main.INSTANCE.getPlayerHandler().onPlayerDeath(instance);
    }
}
