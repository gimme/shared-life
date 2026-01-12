package dev.gimme.sharedlife;

import dev.gimme.sharedlife.domain.config.ServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeServerConfig extends ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SHARE_HEALTH = BUILDER
            .comment("If health should be shared among players")
            .define("shareHealth", true);

    private static final ModConfigSpec.BooleanValue SHARE_HUNGER = BUILDER
            .comment("If hunger should be shared among players")
            .define("shareHunger", true);

    private static final ModConfigSpec.BooleanValue SHARE_EXPERIENCE = BUILDER
            .comment("If experience should be shared among players")
            .define("shareExperience", false);

    private static final ModConfigSpec.BooleanValue ANNOUNCE_DAMAGE = BUILDER
            .comment("If damage events should be announced in chat")
            .define("announceDamage", true);

    private static final ModConfigSpec.BooleanValue INCLUDE_DAMAGE_SOURCE = BUILDER
            .comment("If the source of the damage should be included in announcements")
            .define("includeDamageSource", true);

    static final ModConfigSpec SPEC = BUILDER.build();

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
