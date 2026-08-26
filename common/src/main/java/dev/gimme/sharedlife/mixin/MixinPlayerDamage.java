package dev.gimme.sharedlife.mixin;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets the mod know when a player is damaged.
 *
 * <p>Anchored on the {@code CombatTracker.recordDamage} call in {@code actuallyHurt} because its argument is the
 * final health-reducing damage (armor, enchantments and absorption applied) that carries those same semantics
 * in both vanilla and NeoForge's reshaped bytecode.
 */
@Mixin(Player.class)
public class MixinPlayerDamage {

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"), index = 1, require = 1)
    private float applySharedDamage(DamageSource source, float damage) {
        Player instance = (Player) (Object) this;
        if (instance instanceof ServerPlayer serverPlayer) {
            Main.INSTANCE.getPlayerHandler().onPlayerDamage(serverPlayer, source, damage);
        }

        return damage;
    }
}
