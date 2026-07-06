package dev.gimme.sharedlife.domain.util;

import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for the Player class.
 */
public class Players {

    public static boolean isEthereal(@NotNull ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    public static boolean isSharedHealthEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return Main.INSTANCE.getServerConfig().shareHealth();
    }

    public static boolean isSharedDeathEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return Main.INSTANCE.getServerConfig().shareHealth() || Main.INSTANCE.getServerConfig().shareDeath();
    }

    public static boolean isSharedHungerEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return Main.INSTANCE.getServerConfig().shareHunger();
    }

    /**
     * Whether this player's natural regeneration is combined into the group's single, everyone-must-be-fed
     * regeneration (managed by {@code SharedLife}) instead of running individually.
     *
     * <p>Only applies while health is shared but hunger is not: without a shared health bar there is
     * nothing to combine into, and with a shared hunger bar the one bar already gates regeneration
     * for everyone.
     */
    public static boolean isCombinedNaturalRegenerationEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        ServerConfig config = Main.INSTANCE.getServerConfig();
        return config.shareHealth() && !config.shareHunger() && config.combineNaturalRegeneration();
    }

    public static boolean isSharedExperienceEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return Main.INSTANCE.getServerConfig().shareExperience();
    }
}
