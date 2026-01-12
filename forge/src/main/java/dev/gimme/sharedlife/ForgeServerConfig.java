package dev.gimme.sharedlife;

import dev.gimme.sharedlife.domain.config.ServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ForgeServerConfig extends ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SHARE_HEALTH = BUILDER
            .comment("If health should be shared among players")
            .define("shareHealth", true);

    private static final ForgeConfigSpec.BooleanValue SHARE_HUNGER = BUILDER
            .comment("If hunger should be shared among players")
            .define("shareHunger", true);

    private static final ForgeConfigSpec.BooleanValue SHARE_EXPERIENCE = BUILDER
            .comment("If experience should be shared among players")
            .define("shareExperience", false);

    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_DAMAGE = BUILDER
            .comment("If damage events should be announced in chat")
            .define("announceDamage", true);

    private static final ForgeConfigSpec.BooleanValue INCLUDE_DAMAGE_SOURCE = BUILDER
            .comment("If the source of the damage should be included in announcements")
            .define("includeDamageSource", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

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
