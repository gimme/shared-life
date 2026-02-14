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

    public static boolean isSharedExperienceEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return Main.INSTANCE.getServerConfig().shareExperience();
    }
}
