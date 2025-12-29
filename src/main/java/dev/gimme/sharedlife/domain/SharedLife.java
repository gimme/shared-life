package dev.gimme.sharedlife.domain;

import dev.gimme.sharedlife.domain.config.PlayerSyncStatusChecker;
import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a shared life among all players.
 */
public class SharedLife {

    private final PlayerSyncStatusChecker playerSyncStatusChecker;
    private final ThirstPlugin thirstPlugin;

    private boolean initialized = false;

    private float health;
    private int food;
    private float saturation;
    private int thirst;
    private int quenched;

    private float previousHealth;
    private int previousFood;
    private float previousSaturation;
    private int previousThirst;
    private int previousQuenched;

    public SharedLife(PlayerSyncStatusChecker playerSyncStatusChecker, ThirstPlugin thirstPlugin) {
        this.playerSyncStatusChecker = playerSyncStatusChecker;
        this.thirstPlugin = thirstPlugin;
    }

    /**
     * Initializes the shared life with the state of the given player.
     */
    public void initialize(Player player) {
        this.initialized = true;
        this.health = player.getHealth();
        this.food = player.getFoodData().getFoodLevel();
        this.saturation = player.getFoodData().getSaturationLevel();
        this.thirst = thirstPlugin.getThirst(player);
        this.quenched = thirstPlugin.getQuenched(player);

        resetPreviousStats();
    }

    private void resetPreviousStats() {
        this.previousHealth = this.health;
        this.previousFood = this.food;
        this.previousSaturation = this.saturation;
        this.previousThirst = this.thirst;
        this.previousQuenched = this.quenched;
    }

    /**
     * Ticks the shared life, syncing the state to all players.
     */
    public void tick(PlayerList playerList) {
        for (var player : playerList.getPlayers()) {
            if (player.isDeadOrDying()) continue;
            syncToPlayer(player);
        }

        resetPreviousStats();
    }

    /**
     * Syncs the shared life state to the given player.
     */
    public void syncToPlayer(Player player) {
        var playerFoodData = player.getFoodData();
        var playerSyncedStats = playerSyncStatusChecker.getPlayerSyncedStats(player);

        if (playerSyncedStats.health()) player.setHealth(this.health);
        if (playerSyncedStats.food()) playerFoodData.setFoodLevel(this.food);
        if (playerSyncedStats.saturation()) playerFoodData.setSaturation(this.saturation);

        if (playerSyncedStats.thirst()) thirstPlugin.setThirst(player, this.thirst);
        if (playerSyncedStats.quenched()) thirstPlugin.setQuenched(player, this.quenched);
    }

    /**
     * Updates the shared life with the changes from the given player.
     */
    public void updateFromPlayer(Player player) {
        var playerFoodData = player.getFoodData();

        var healthChange = player.getHealth() - previousHealth;
        var foodChange = playerFoodData.getFoodLevel() - previousFood;
        var saturationChange = playerFoodData.getSaturationLevel() - previousSaturation;
        var thirstChange = thirstPlugin.getThirst(player) - previousThirst;
        var quenchedChange = thirstPlugin.getQuenched(player) - previousQuenched;

        this.health = Math.max(0, this.health + healthChange);
        this.food = Math.max(0, this.food + foodChange);
        this.saturation = Math.max(0, this.saturation + saturationChange);
        this.thirst = Math.max(0, this.thirst + thirstChange);
        this.quenched = Math.max(0, this.quenched + quenchedChange);
    }

    /**
     * Kills the shared life.
     */
    public void kill() {
        this.health = 0;
    }

    /**
     * Checks if the shared life is dead.
     */
    public boolean isDead() {
        return health <= 0;
    }

    /**
     * Checks if the shared life has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}
