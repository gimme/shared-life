package dev.gimme.sharedlife.forge.plugins;

import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Thirst plugin implementation for Forge using reflection to access the "Thirst Was Taken" mod.
 */
public class ForgeThirstPlugin implements ThirstPlugin {

    private Function<@NotNull Player, @Nullable Object> getThirstCapabilityFunction = (player) -> null;
    private Method getThirstMethod = null;
    private Method setThirstMethod = null;
    private Method getQuenchedMethod = null;
    private Method setQuenchedMethod = null;

    public ForgeThirstPlugin() {
        if (!ModList.get().isLoaded("thirst")) return;

        try {
            var modCapabilitiesClass = Class.forName("dev.ghen.thirst.foundation.common.capability.ModCapabilities");
            var thirstCapabilityField = modCapabilitiesClass.getField("PLAYER_THIRST");
            var thirstCapability = (Capability<?>) thirstCapabilityField.get(null);

            var thirstCapabilityClass = Class.forName("dev.ghen.thirst.foundation.common.capability.IThirst");

            getThirstCapabilityFunction = (player) -> player.getCapability(thirstCapability).resolve().orElse(null);
            getThirstMethod = thirstCapabilityClass.getMethod("getThirst");
            setThirstMethod = thirstCapabilityClass.getMethod("setThirst", int.class);
            getQuenchedMethod = thirstCapabilityClass.getMethod("getQuenched");
            setQuenchedMethod = thirstCapabilityClass.getMethod("setQuenched", int.class);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int getThirst(@NotNull Player player) {
        var thirstCap = getThirstCapabilityFunction.apply(player);
        if (thirstCap == null || getThirstMethod == null) return 0;
        try {
            return (int) getThirstMethod.invoke(thirstCap);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setThirst(@NotNull Player player, int thirst) {
        var thirstCap = getThirstCapabilityFunction.apply(player);
        if (thirstCap == null || setThirstMethod == null) return;
        try {
            setThirstMethod.invoke(thirstCap, thirst);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int getQuenched(@NotNull Player player) {
        var thirstCap = getThirstCapabilityFunction.apply(player);
        if (thirstCap == null || getQuenchedMethod == null) return 0;
        try {
            return (int) getQuenchedMethod.invoke(thirstCap);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setQuenched(@NotNull Player player, int quenched) {
        var thirstCap = getThirstCapabilityFunction.apply(player);
        if (thirstCap == null || setQuenchedMethod == null) return;
        try {
            setQuenchedMethod.invoke(thirstCap, quenched);
        } catch (Exception ignored) {
        }
    }
}
