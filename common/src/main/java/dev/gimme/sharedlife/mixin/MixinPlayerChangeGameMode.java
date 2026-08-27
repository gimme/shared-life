package dev.gimme.sharedlife.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the mod know when a player has changed their game mode.
 *
 * <p>Injected at {@code RETURN} — after the new mode is applied — so the handler's creative/spectator checks
 * read the mode the player switched <em>into</em>, not the one they left. {@code setGameMode} returns false when
 * nothing changed (same mode, or a NeoForge listener canceled the event), so those calls are ignored.
 */
@Mixin(ServerPlayer.class)
public class MixinPlayerChangeGameMode {

    @Inject(method = "setGameMode", at = @At(value = "RETURN"), require = 1)
    private void onChangeGameMode(GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ServerPlayer instance = (ServerPlayer) (Object) this;
        Main.INSTANCE.getPlayerHandler().onPlayerChangeGameMode(instance);
    }
}
