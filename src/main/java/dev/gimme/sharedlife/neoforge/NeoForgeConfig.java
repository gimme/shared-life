package dev.gimme.sharedlife.neoforge;

import dev.gimme.sharedlife.domain.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig implements Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SYNC_HEALTH = BUILDER
            .comment("If health should be synced between players")
            .define("syncHealth", true);

    private static final ModConfigSpec.BooleanValue SYNC_ABSORPTION = BUILDER
            .comment("If absorption should be synced between players")
            .define("syncAbsorption", true);

    private static final ModConfigSpec.BooleanValue SYNC_FOOD = BUILDER
            .comment("If food level should be synced between players")
            .define("syncFood", true);

    private static final ModConfigSpec.BooleanValue SYNC_SATURATION = BUILDER
            .comment("If saturation should be synced between players")
            .define("syncSaturation", true);

    private static final ModConfigSpec.BooleanValue SYNC_THIRST = BUILDER
            .comment("[Thirst Was Taken] If thirst should be synced between players")
            .define("syncThirst", true);

    private static final ModConfigSpec.BooleanValue SYNC_QUENCHED = BUILDER
            .comment("[Thirst Was Taken] If quenched should be synced between players")
            .define("syncQuenched", true);

    private static final ModConfigSpec.BooleanValue SYNC_EXPERIENCE = BUILDER
            .comment("If experience should be synced between players")
            .define("syncExperience", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    @Override
    public boolean syncHealth() {
        return SYNC_HEALTH.get();
    }

    @Override
    public boolean syncAbsorption() {
        return SYNC_ABSORPTION.get();
    }

    @Override
    public boolean syncFood() {
        return SYNC_FOOD.get();
    }

    @Override
    public boolean syncSaturation() {
        return SYNC_SATURATION.get();
    }

    @Override
    public boolean syncThirst() {
        return SYNC_THIRST.get();
    }

    @Override
    public boolean syncQuenched() {
        return SYNC_QUENCHED.get();
    }

    @Override
    public boolean syncExperience() {
        return SYNC_EXPERIENCE.get();
    }
}
