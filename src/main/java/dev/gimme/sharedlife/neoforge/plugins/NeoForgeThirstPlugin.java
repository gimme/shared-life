package dev.gimme.sharedlife.neoforge.plugins;

import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class NeoForgeThirstPlugin implements ThirstPlugin {

    @Override
    public int getThirst(@NotNull Player player) {
        return 0;
    }

    @Override
    public void setThirst(@NotNull Player player, int thirst) {
    }

    @Override
    public int getQuenched(@NotNull Player player) {
        return 0;
    }

    @Override
    public void setQuenched(@NotNull Player player, int quenched) {
    }
}
