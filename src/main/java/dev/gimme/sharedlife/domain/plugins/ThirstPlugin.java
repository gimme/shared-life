package dev.gimme.sharedlife.domain.plugins;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin interface for accessing stats introduced by thirst mods.
 */
public interface ThirstPlugin {

    int getThirst(@NotNull Player player);

    void setThirst(@NotNull Player player, int thirst);

    int getQuenched(@NotNull Player player);

    void setQuenched(@NotNull Player player, int quenched);
}
