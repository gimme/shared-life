package dev.gimme.sharedlife.application;

import dev.gimme.sharedlife.domain.SharedLife;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

public class PlayerHandler {

    private final SharedLife sharedLife;

    public PlayerHandler(SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    public void onPlayerJoinLevel(@NotNull ServerPlayer player) {
        sharedLife.includePotentialNewPlayer(player);
    }

    public void onPlayerChangeGameMode(@NotNull ServerPlayer player) {
        sharedLife.includePotentialNewPlayer(player);
    }

    public void onPlayerDamage(@NotNull ServerPlayer player, DamageSource source, float amount) {
        sharedLife.hurtByPlayer(player, source, amount);
    }

    public void onPlayerHeal(@NotNull ServerPlayer player, float amount) {
        sharedLife.healByPlayer(player, amount);
    }

    public void onPlayerDeath(@NotNull ServerPlayer player) {
        sharedLife.killBy(player);
    }
}
