package dev.gimme.sharedlife.infrastructure;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Test-only handles to {@link FcapServerConfig} values. Lives in the gametest source set's
 * {@code infrastructure} package so it can reach the package-private config fields; production code
 * still exposes only the read-only getters.
 */
public final class ConfigTestSupport {

    public static final BooleanValue SHARE_HEALTH = FcapServerConfig.SHARE_HEALTH;
    public static final BooleanValue SHARE_DEATH = FcapServerConfig.SHARE_DEATH;
    public static final BooleanValue SHARE_HUNGER = FcapServerConfig.SHARE_HUNGER;
    public static final BooleanValue COMBINE_NATURAL_REGENERATION = FcapServerConfig.COMBINE_NATURAL_REGENERATION;
    public static final BooleanValue SHARE_EXPERIENCE = FcapServerConfig.SHARE_EXPERIENCE;
    public static final BooleanValue ANNOUNCE_DAMAGE = FcapServerConfig.ANNOUNCE_DAMAGE;
    public static final BooleanValue INCLUDE_DAMAGE_SOURCE = FcapServerConfig.INCLUDE_DAMAGE_SOURCE;
    public static final BooleanValue ANNOUNCE_DEATH_SUMMARY = FcapServerConfig.ANNOUNCE_DEATH_SUMMARY;

    private ConfigTestSupport() {
    }

    /** A restore handle whose {@code close()} throws nothing, so it reads cleanly in try-with-resources. */
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Mutates the live config and returns a scope that restores the previous value on close, keeping tests isolated.
     */
    public static <T> Scope override(ConfigValue<T> config, T value) {
        T previous = config.get();
        config.set(value);
        return () -> config.set(previous);
    }
}
