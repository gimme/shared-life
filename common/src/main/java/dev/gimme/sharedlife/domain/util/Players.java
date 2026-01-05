package dev.gimme.sharedlife.domain.util;

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
        return ServerConfig.INSTANCE.shareHealth();
    }

    public static boolean isSharedHungerEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return ServerConfig.INSTANCE.shareHunger();
    }

    public static boolean isSharedExperienceEnabled(@NotNull ServerPlayer player) {
        if (isEthereal(player)) return false;
        return ServerConfig.INSTANCE.shareExperience();
    }
}
