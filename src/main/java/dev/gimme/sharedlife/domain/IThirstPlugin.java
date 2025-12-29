package dev.gimme.sharedlife.domain;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public interface IThirstPlugin {

    int getThirst(@NotNull Player player);

    void setThirst(@NotNull Player player, int thirst);

    int getQuenched(@NotNull Player player);

    void setQuenched(@NotNull Player player, int quenched);
}
