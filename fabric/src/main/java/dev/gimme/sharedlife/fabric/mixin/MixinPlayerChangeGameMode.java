package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the mod know when a player changes their game mode.
 */
@Mixin(ServerPlayer.class)
public class MixinPlayerChangeGameMode {

    @Inject(method = "setGameMode", at = @At(value = "HEAD"), require = 1)
    private void onChangeGameMode(GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer instance = (ServerPlayer) (Object) this;
        Main.INSTANCE.getPlayerHandler().onPlayerChangeGameMode(instance, gameMode);
    }
}
