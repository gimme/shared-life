package dev.gimme.sharedlife.domain;

import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.config.PlayerSyncStatusChecker;
import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import net.minecraft.core.Holder;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Represents a shared life among all players.
 */
public class SharedLife {

    public static final DamageType DEATH_TYPE = new DamageType("sharedlife_death", 0);
    public static final DamageSource DEATH_SOURCE = new DamageSource(Holder.direct(DEATH_TYPE));

    private static final Logger LOG = LogUtils.getLogger();

    private final PlayerSyncStatusChecker playerSyncStatusChecker;
    private final ThirstPlugin thirstPlugin;

    private float health;
    private float absorption;
    private int food;
    private float saturation;
    private int thirst;
    private int quenched;
    private int experience;

    private float previousHealth;
    private float previousAbsorption;
    private int previousFood;
    private float previousSaturation;
    private int previousThirst;
    private int previousQuenched;
    private int previousExperience;

    public SharedLife(PlayerSyncStatusChecker playerSyncStatusChecker, ThirstPlugin thirstPlugin) {
        this.playerSyncStatusChecker = playerSyncStatusChecker;
        this.thirstPlugin = thirstPlugin;
    }

    /**
     * Initializes the shared life with the state of the given player.
     */
    public void initializeFrom(Player player) {
        this.health = player.getHealth();
        this.absorption = player.getAbsorptionAmount();
        this.food = player.getFoodData().getFoodLevel();
        this.saturation = player.getFoodData().getSaturationLevel();
        this.thirst = thirstPlugin.getThirst(player);
        this.quenched = thirstPlugin.getQuenched(player);
        this.experience = player.totalExperience;

        resetPreviousStats();
        LOG.debug("Initialized shared life from player {}: {}", player.getName().getString(), this);
    }

    private void resetPreviousStats() {
        this.previousHealth = this.health;
        this.previousAbsorption = this.absorption;
        this.previousFood = this.food;
        this.previousSaturation = this.saturation;
        this.previousThirst = this.thirst;
        this.previousQuenched = this.quenched;
        this.previousExperience = this.experience;
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
    public void syncToPlayer(Player player) {
        if (isExemptFromSharedLife(player)) return;

        var playerSyncedStats = playerSyncStatusChecker.getPlayerSyncedStats(player);
        var playerFoodData = player.getFoodData();

        if (playerSyncedStats.health()) {
            player.setHealth(this.health);
            if (isDead()) {
                player.die(DEATH_SOURCE);
            }
        }
        if (playerSyncedStats.absorption()) player.setAbsorptionAmount(this.absorption);
        if (playerSyncedStats.food()) playerFoodData.setFoodLevel(this.food);
        if (playerSyncedStats.saturation()) playerFoodData.setSaturation(this.saturation);

        if (playerSyncedStats.thirst()) thirstPlugin.setThirst(player, this.thirst);
        if (playerSyncedStats.quenched()) thirstPlugin.setQuenched(player, this.quenched);

        if (playerSyncedStats.experience()) player.giveExperiencePoints(this.experience - player.totalExperience);
    }

    /**
     * Updates the shared life with the changes from the given player.
     */
    public void applyChangesFrom(Player player) {
        if (isExemptFromSharedLife(player)) return;

        var playerSyncedStats = playerSyncStatusChecker.getPlayerSyncedStats(player);
        var playerFoodData = player.getFoodData();

        var healthChange = player.getHealth() - previousHealth;
        var absorptionChange = player.getAbsorptionAmount() - previousAbsorption;
        var foodChange = playerFoodData.getFoodLevel() - previousFood;
        var saturationChange = playerFoodData.getSaturationLevel() - previousSaturation;
        var thirstChange = thirstPlugin.getThirst(player) - previousThirst;
        var quenchedChange = thirstPlugin.getQuenched(player) - previousQuenched;
        var experienceChange = player.totalExperience - previousExperience;

        if (playerSyncedStats.health()) this.health = Math.max(0, this.health + healthChange);
        if (playerSyncedStats.absorption()) this.absorption = Math.max(0, this.absorption + absorptionChange);
        if (playerSyncedStats.food()) this.food = Math.max(0, this.food + foodChange);
        if (playerSyncedStats.saturation()) this.saturation = Math.max(0, this.saturation + saturationChange);
        if (playerSyncedStats.thirst()) this.thirst = Math.max(0, this.thirst + thirstChange);
        if (playerSyncedStats.quenched()) this.quenched = Math.max(0, this.quenched + quenchedChange);
        if (playerSyncedStats.experience()) this.experience = Math.max(0, this.experience + experienceChange);

        LOG.trace("Applied changes from player {}: {}", player.getName().getString(), this);
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
     * Checks if the given player is currently exempt from shared life effects.
     */
    private boolean isExemptFromSharedLife(Player player) {
        if (player.isDeadOrDying()) return true;
        if (player.isSpectator()) return true;
        if (player.isCreative()) return true;

        return false;
    }

    @Override
    public String toString() {
        return "SharedLife(health=%s, absorption=%s, food=%s, saturation=%s, thirst=%s, quenched=%s, experience=%s)"
                .formatted(health, absorption, food, saturation, thirst, quenched, experience);
    }
}
