package dev.gimme.sharedlife.fabric.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lets the mod know when a player is damaged.
 */
@Mixin(Player.class)
public class MixinPlayerDamage {

    @ModifyVariable(method = "actuallyHurt", at = @At(value= "INVOKE_ASSIGN", target = "Ljava/lang/Math;max(FF)F", ordinal = 0), ordinal = 0, argsOnly = true, require = 1)
    private float applySharedDamage(float g, ServerLevel level, DamageSource source, float damage) {
        Player instance = (Player) (Object) this;
        if (instance instanceof ServerPlayer serverPlayer) {
            Main.INSTANCE.getPlayerHandler().onPlayerDamage(serverPlayer, source, g);
        }

        return g;
    }
}
