package dev.gimme.sharedlife.domain.config;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

public class PlayerSyncStatusChecker {

    private final Config config;

    public PlayerSyncStatusChecker(Config config) {
        this.config = config;
    }

    public PlayerSyncedStats getPlayerSyncedStats(Player player) {
        return new PlayerSyncedStats(
                config.syncHealth() || checkGameRule(player, ModGameRules.SYNC_HEALTH),
                config.syncFood() || checkGameRule(player, ModGameRules.SYNC_FOOD),
                config.syncSaturation() || checkGameRule(player, ModGameRules.SYNC_SATURATION),
                config.syncThirst() || checkGameRule(player, ModGameRules.SYNC_THIRST),
                config.syncQuenched() || checkGameRule(player, ModGameRules.SYNC_QUENCHED),
                config.syncExperience() || checkGameRule(player, ModGameRules.SYNC_EXPERIENCE)
        );
    }

    private boolean checkGameRule(Player player, GameRules.Key<GameRules.BooleanValue> rule) {
        var gameRule = player.level().getGameRules().getRule(rule);
        if (gameRule == null) return false;
        return gameRule.get();
    }
}
