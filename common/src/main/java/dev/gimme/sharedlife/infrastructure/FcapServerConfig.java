package dev.gimme.sharedlife.infrastructure;

import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/**
 * {@link ServerConfig} backed by the NeoForge config system. The spec is defined once here in the common module and
 * registered per loader (natively on NeoForge, via Forge Config API Port on Fabric) as a {@code COMMON} config.
 */
public class FcapServerConfig extends ServerConfig {

    public static final String FILE_NAME = Constants.MOD_ID + "-server.toml";
    public static final ModConfigSpec SPEC;

    // Package-private (not private) so the gametest source set's ConfigTestSupport, which lives in
    // this same package, can override these in tests. Production code still reads them only via the
    // getters below.
    static final BooleanValue SHARE_HEALTH;
    static final BooleanValue SHARE_DEATH;
    static final BooleanValue SHARE_HUNGER;
    static final BooleanValue COMBINE_NATURAL_REGENERATION;
    static final BooleanValue SHARE_EXPERIENCE;
    static final BooleanValue ANNOUNCE_DAMAGE;
    static final BooleanValue INCLUDE_DAMAGE_SOURCE;
    static final BooleanValue ANNOUNCE_DEATH_SUMMARY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        SHARE_HEALTH = builder
            .comment("If health should be shared among players")
            .define("shareHealth", true);

        SHARE_HUNGER = builder
            .comment("If hunger should be shared among players")
            .define("shareHunger", false);

        COMBINE_NATURAL_REGENERATION = builder
            .comment("""
                If natural regeneration should be a group effort.
                With this, the shared life regenerates only while every player stays fed, and everyone pays the same hunger cost.
                If disabled, each fed player's own regeneration heals the shared life separately, which can be quite strong.
                Has no effect when shareHunger is enabled, because it already works like that.""")
            .define("combineNaturalRegeneration", true);

        SHARE_EXPERIENCE = builder
            .comment("If experience should be shared among players")
            .define("shareExperience", false);

        SHARE_DEATH = builder
            .comment("If all players should die when one player dies. This implicitly happens if shareHealth is enabled.")
            .define("shareDeath", false);

        ANNOUNCE_DAMAGE = builder
            .comment("If damage events should be announced in chat")
            .define("announceDamage", true);

        INCLUDE_DAMAGE_SOURCE = builder
            .comment("If the source of the damage should be included in announcements")
            .define("includeDamageSource", true);

        ANNOUNCE_DEATH_SUMMARY = builder
            .comment("If a summary of how much damage each player took should be announced in chat when the shared life ends")
            .define("announceDeathSummary", true);

        SPEC = builder.build();
    }

    @Override
    public boolean shareHealth() {
        return SHARE_HEALTH.get();
    }

    @Override
    public boolean shareDeath() {
        return SHARE_DEATH.get();
    }

    @Override
    public boolean shareHunger() {
        return SHARE_HUNGER.get();
    }

    @Override
    public boolean combineNaturalRegeneration() {
        return COMBINE_NATURAL_REGENERATION.get();
    }

    @Override
    public boolean shareExperience() {
        return SHARE_EXPERIENCE.get();
    }

    @Override
    public boolean announceDamage() {
        return ANNOUNCE_DAMAGE.get();
    }

    @Override
    public boolean includeDamageSource() {
        return INCLUDE_DAMAGE_SOURCE.get();
    }

    @Override
    public boolean announceDeathSummary() {
        return ANNOUNCE_DEATH_SUMMARY.get();
    }
}
