package dev.gimme.sharedlife.domain.config;

import net.minecraft.world.level.GameRules;

class ModGameRules {

    static final GameRules.Key<GameRules.BooleanValue> SYNC_HEALTH =
            GameRules.register("syncHealth", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    static final GameRules.Key<GameRules.BooleanValue> SYNC_FOOD =
            GameRules.register("syncFood", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    static final GameRules.Key<GameRules.BooleanValue> SYNC_SATURATION =
            GameRules.register("syncSaturation", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    static final GameRules.Key<GameRules.BooleanValue> SYNC_THIRST =
            GameRules.register("syncThirst", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    static final GameRules.Key<GameRules.BooleanValue> SYNC_QUENCHED =
            GameRules.register("syncQuenched", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    static final GameRules.Key<GameRules.BooleanValue> SYNC_EXPERIENCE =
            GameRules.register("syncExperience", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
}
