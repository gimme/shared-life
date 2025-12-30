package dev.gimme.sharedlife.domain;

import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.config.PlayerConfig;
import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;

/**
 * Represents a shared life among all players.
 */
public class SharedLife {

    private static final Logger LOG = LogUtils.getLogger();

    private final PlayerConfig playerConfig;
    private final ThirstPlugin thirstPlugin;

    private float health;
    private int food;
    private float saturation;
    private int thirst;
    private int quenched;
    private int experienceLevel;

    private float previousHealth;
    private int previousFood;
    private float previousSaturation;
    private int previousThirst;
    private int previousQuenched;
    private int previousExperienceLevel;

    public SharedLife(PlayerConfig playerConfig, ThirstPlugin thirstPlugin) {
        this.playerConfig = playerConfig;
        this.thirstPlugin = thirstPlugin;
    }

    /**
     * Includes a new player into the shared life.
     */
    public void includeNewPlayer(ServerPlayer player) {
        if (isExemptFromSharedLife(player)) return;

        if (isDead()) {
            initializeFrom(player);
        }

        syncToPlayer(player);
    }

    /**
     * Initializes the shared life with the state of the given player.
     */
    private void initializeFrom(ServerPlayer player) {
        this.health = player.getHealth();
        this.food = player.getFoodData().getFoodLevel();
        this.saturation = player.getFoodData().getSaturationLevel();
        this.thirst = thirstPlugin.getThirst(player);
        this.quenched = thirstPlugin.getQuenched(player);
        this.experienceLevel = player.experienceLevel;

        resetPreviousStats();
        LOG.debug("Initialized shared life from player {}: {}", player.getName().getString(), this);
    }

    private void resetPreviousStats() {
        this.previousHealth = this.health;
        this.previousFood = this.food;
        this.previousSaturation = this.saturation;
        this.previousThirst = this.thirst;
        this.previousQuenched = this.quenched;
        this.previousExperienceLevel = this.experienceLevel;
    }

    /**
     * Ticks the shared life, syncing the state to all players.
     */
    public void tick(PlayerList playerList) {
        LOG.trace("Tick: {}", this);

        for (var player : playerList.getPlayers()) {
            syncToPlayer(player);
        }

        resetPreviousStats();
    }

    /**
     * Syncs the shared life state to the given player.
     */
    public void syncToPlayer(ServerPlayer player) {
        if (isExemptFromSharedLife(player)) return;

        var enabledStats = playerConfig.getEnabledSyncStats(player);
        var playerFoodData = player.getFoodData();

        if (enabledStats.health()) {
            player.setHealth(this.health);
            if (isDead()) {
                var genericDamageSource = player.level().damageSources().generic();
                player.die(genericDamageSource);
            }
        }
        if (enabledStats.food()) playerFoodData.setFoodLevel(this.food);
        if (enabledStats.saturation()) playerFoodData.setSaturation(this.saturation);

        if (enabledStats.thirst()) thirstPlugin.setThirst(player, this.thirst);
        if (enabledStats.quenched()) thirstPlugin.setQuenched(player, this.quenched);

        if (enabledStats.experience()) player.giveExperienceLevels(this.experienceLevel - player.experienceLevel);
    }

    /**
     * Updates the shared life with the changes from the given player.
     */
    public void applyChangesFrom(ServerPlayer player) {
        if (isExemptFromSharedLife(player)) return;

        var playerSyncedStats = playerConfig.getEnabledSyncStats(player);
        var playerFoodData = player.getFoodData();

        var healthChange = player.getHealth() - previousHealth;
        var foodChange = playerFoodData.getFoodLevel() - previousFood;
        var saturationChange = playerFoodData.getSaturationLevel() - previousSaturation;
        var thirstChange = thirstPlugin.getThirst(player) - previousThirst;
        var quenchedChange = thirstPlugin.getQuenched(player) - previousQuenched;
        var experienceLevelChange = player.experienceLevel - previousExperienceLevel;

        if (playerSyncedStats.health()) this.health = Math.max(0, this.health + healthChange);
        if (playerSyncedStats.food()) this.food = Math.max(0, this.food + foodChange);
        if (playerSyncedStats.saturation()) this.saturation = Math.max(0, this.saturation + saturationChange);
        if (playerSyncedStats.thirst()) this.thirst = Math.max(0, this.thirst + thirstChange);
        if (playerSyncedStats.quenched()) this.quenched = Math.max(0, this.quenched + quenchedChange);
        if (playerSyncedStats.experience()) this.experienceLevel = Math.max(0, this.experienceLevel + experienceLevelChange);

        LOG.trace("Applied changes from player {}: {}", player.getName().getString(), this);
    }

    /**
     * Ends the shared life due to the given player's death.
     */
    public void endIt(ServerPlayer player) {
        if (isDead()) return;
        if (playerConfig.getEnabledSyncStats(player).health()) {
            this.health = 0;
            LOG.info("{} has caused shared life death.", player.getName().getString());
        }
    }

    /**
     * Checks if the shared life is dead.
     */
    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public String toString() {
        return "SharedLife(health=%s, food=%s, saturation=%s, thirst=%s, quenched=%s, experienceLevel=%s)"
                .formatted(health, food, saturation, thirst, quenched, experienceLevel);
    }

    /**
     * Checks if the given player is currently exempt from shared life effects.
     */
    private static boolean isExemptFromSharedLife(ServerPlayer player) {
        if (player.isDeadOrDying()) return true;
        if (player.isSpectator()) return true;
        if (player.isCreative()) return true;

        return false;
    }
}
