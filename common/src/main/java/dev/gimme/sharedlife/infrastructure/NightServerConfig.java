package dev.gimme.sharedlife.infrastructure;

import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.infrastructure.ModConfigSpec.ConfigValue;

public class NightServerConfig extends ServerConfig {

    public static final ModConfigSpec SPEC = new ModConfigSpec();

    private static final ConfigValue<Boolean> SHARE_HEALTH = SPEC.variable()
            .comment("If health should be shared among players")
            .define("shareHealth", true);

    private static final ConfigValue<Boolean> SHARE_HUNGER = SPEC.variable()
            .comment("If hunger should be shared among players")
            .define("shareHunger", true);

    private static final ConfigValue<Boolean> SHARE_EXPERIENCE = SPEC.variable()
            .comment("If experience should be shared among players")
            .define("shareExperience", false);

    private static final ConfigValue<Boolean> ANNOUNCE_DAMAGE = SPEC.variable()
            .comment("If damage events should be announced in chat")
            .define("announceDamage", true);

    private static final ConfigValue<Boolean> INCLUDE_DAMAGE_SOURCE = SPEC.variable()
            .comment("If the source of the damage should be included in announcements")
            .define("includeDamageSource", true);

    @Override
    public boolean shareHealth() {
        return SHARE_HEALTH.get();
    }

    @Override
    public boolean shareHunger() {
        return SHARE_HUNGER.get();
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
}
