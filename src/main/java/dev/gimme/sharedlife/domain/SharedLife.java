package dev.gimme.sharedlife.domain;

import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a shared life among all players.
 */
public class SharedLife {

    private boolean initialized = false;

    private float health;
    private int food;
    private float saturation;

    private float previousHealth;
    private int previousFood;
    private float previousSaturation;

    /**
     * Initializes the shared life with the state of the given player.
     */
    public void initialize(Player player) {
        this.initialized = true;
        this.health = player.getHealth();
        this.food = player.getFoodData().getFoodLevel();
        this.saturation = player.getFoodData().getSaturationLevel();

        this.previousHealth = this.health;
        this.previousFood = this.food;
        this.previousSaturation = this.saturation;
    }

    /**
     * Ticks the shared life, syncing the state to all players.
     */
    public void tick(PlayerList playerList) {
        for (var player : playerList.getPlayers()) {
            if (player.isDeadOrDying()) continue;
            syncToPlayer(player);
        }

        this.previousHealth = this.health;
        this.previousFood = this.food;
        this.previousSaturation = this.saturation;
    }

    /**
     * Syncs the shared life state to the given player.
     */
    public void syncToPlayer(Player player) {
        player.setHealth(this.health);
        var playerFoodData = player.getFoodData();
        playerFoodData.setFoodLevel(this.food);
        playerFoodData.setSaturation(this.saturation);
    }

    /**
     * Updates the shared life with the changes from the given player.
     */
    public void updateFromPlayer(Player player) {
        var playerFoodData = player.getFoodData();

        var healthChange = player.getHealth() - previousHealth;
        var foodChange = playerFoodData.getFoodLevel() - previousFood;
        var saturationChange = playerFoodData.getSaturationLevel() - previousSaturation;

        this.health = Math.max(0, this.health + healthChange);
        this.food = Math.max(0, this.food + foodChange);
        this.saturation = Math.max(0, this.saturation + saturationChange);
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
