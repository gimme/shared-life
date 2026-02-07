package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the mod know when a player joins a level, which includes respawning after death and joining the world for the first time.
 */
@Mixin(ServerLevel.class)
public class MixinPlayerRespawn {

    @Inject(method = "addPlayer", at = @At(value= "HEAD"), require = 1)
    private void onPlayerJoinLevel(ServerPlayer player, CallbackInfo ci) {
        Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(player);
    }
}
