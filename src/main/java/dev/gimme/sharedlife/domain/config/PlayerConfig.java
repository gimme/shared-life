package dev.gimme.sharedlife.domain.config;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

/**
 * Handles player-specific configuration.
 */
public class PlayerConfig {

    private final Config config;

    public PlayerConfig(Config config) {
        this.config = config;
    }

    /**
     * Returns which stats are currently enabled for syncing for the given player.
     */
    public PlayerEnabledSyncStats getEnabledSyncStats(Player player) {
        return new PlayerEnabledSyncStats(
                config.syncHealth() || checkGameRule(player, ModGameRules.SYNC_HEALTH),
                config.syncFood() || checkGameRule(player, ModGameRules.SYNC_FOOD),
                config.syncSaturation() || checkGameRule(player, ModGameRules.SYNC_SATURATION),
                config.syncThirst() || checkGameRule(player, ModGameRules.SYNC_THIRST),
                config.syncQuenched() || checkGameRule(player, ModGameRules.SYNC_QUENCHED),
                config.syncExperience() || checkGameRule(player, ModGameRules.SYNC_EXPERIENCE)
        );
    }

    /**
     * Checks if a game rule is enabled for the player's current level.
     */
    private boolean checkGameRule(Player player, GameRules.Key<GameRules.BooleanValue> rule) {
        var gameRule = player.level().getGameRules().getRule(rule);
        if (gameRule == null) return false;
        return gameRule.get();
    }
}
