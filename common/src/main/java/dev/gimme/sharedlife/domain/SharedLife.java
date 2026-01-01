package dev.gimme.sharedlife.domain;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.plugins.ThirstPlugin;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.util.UUID;
import java.util.stream.Stream;

public class SharedLife extends FakePlayer {

    public static final String NAME = "Shared Life";
    private static final DecimalFormat HEARTS_DECIMAL_FORMAT = new DecimalFormat("0.0");

    private static final Logger LOG = LogUtils.getLogger();

    private final DamageSource damageSource = new DamageSource(registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModDamageTypes.SHARED_LIFE));
    private final @Nullable ThirstPlugin thirstPlugin;

    private int previousFoodLevel;
    private float previousSaturation;
    private int previousThirst;
    private int previousQuenched;
    private int previousExperienceLevel;

    public SharedLife(@NotNull ServerLevel level) {
        this(level, null);
    }

    public SharedLife(@NotNull ServerLevel level, @Nullable ThirstPlugin thirstPlugin) {
        super(level, new GameProfile(UUID.randomUUID(), NAME));
        this.thirstPlugin = thirstPlugin;
    }

    /**
     * Syncs up a potential new player into the shared life.
     */
    public void includePotentialNewPlayer(ServerPlayer player) {
        if (player.isDeadOrDying()) return;
        if (Players.isEthereal(player)) return;

        if (isDeadOrDying()) {
            initializeFrom(player);
        } else {
            initializeTo(player);
        }
    }

    /**
     * Syncs the shared life state to the given player.
     */
    public void initializeTo(ServerPlayer player) {
        if (player.isDeadOrDying()) return;

        if (Players.isSharedHealthEnabled(player)) {
            player.setHealth(getHealth());

            if (isDeadOrDying()) {
                killPlayer(player);
            }
        }

        if (Players.isSharedHungerEnabled(player)) {
            applyHunger(player);
        }

        if (Players.isSharedExperienceEnabled(player)) {
            player.setExperienceLevels(this.experienceLevel);
        }
    }

    private void applyHunger(ServerPlayer player) {
        player.getFoodData().setFoodLevel(this.foodData.getFoodLevel());
        player.getFoodData().setSaturation(this.foodData.getSaturationLevel());

        if (thirstPlugin != null) {
            thirstPlugin.setThirst(player, thirstPlugin.getThirst(this));
            thirstPlugin.setQuenched(player, thirstPlugin.getQuenched(this));
        }
    }

    /**
     * Initializes the shared life with the state of the given player.
     */
    private void initializeFrom(ServerPlayer player) {
        setHealth(player.getHealth());
        setExperienceLevels(player.experienceLevel);

        this.foodData.setFoodLevel(player.getFoodData().getFoodLevel());
        this.foodData.setSaturation(player.getFoodData().getSaturationLevel());
        this.foodData.addExhaustion(player.getFoodData().getExhaustionLevel());
        player.getFoodData().setExhaustion(0);

        if (thirstPlugin != null) {
            thirstPlugin.setThirst(this, thirstPlugin.getThirst(player));
            thirstPlugin.setQuenched(this, thirstPlugin.getQuenched(player));
        }

        resetPreviousStats();
        LOG.debug("Initialized shared life from player {}: {}", player.getName().getString(), this);
    }

    private void resetPreviousStats() {
        this.previousFoodLevel = this.foodData.getFoodLevel();
        this.previousSaturation = this.foodData.getSaturationLevel();
        if (thirstPlugin != null) {
            this.previousThirst = thirstPlugin.getThirst(this);
            this.previousQuenched = thirstPlugin.getQuenched(this);
        }
        this.previousExperienceLevel = experienceLevel;
    }

    @Override
    public void tick() {
        tickHunger();
        syncExperience();
    }

    private void tickHunger() {
        var hungryPlayers = getLiveSharedHungerPlayers().toList();
        if (hungryPlayers.isEmpty()) return;

        for (ServerPlayer player : hungryPlayers) {
            var foodLevelChange = player.getFoodData().getFoodLevel() - this.previousFoodLevel;
            var saturationChange = player.getFoodData().getSaturationLevel() - this.previousSaturation;
            var exhaustionChange = player.getFoodData().getExhaustionLevel();

            this.foodData.setFoodLevel(Math.max(0, this.foodData.getFoodLevel() + foodLevelChange));
            this.foodData.setSaturation(Math.max(0, this.foodData.getSaturationLevel() + saturationChange));
            this.foodData.addExhaustion(exhaustionChange);
            player.getFoodData().setExhaustion(0);

            if (thirstPlugin != null) {
                var thirstChange = thirstPlugin.getThirst(player) - previousThirst;
                var quenchedChange = thirstPlugin.getQuenched(player) - previousQuenched;
                thirstPlugin.setThirst(this, Math.max(0, thirstPlugin.getThirst(this) + thirstChange));
                thirstPlugin.setQuenched(this, Math.max(0, thirstPlugin.getQuenched(this) + quenchedChange));
            }
        }

        this.foodData.tick(this);

        for (ServerPlayer player : hungryPlayers) {
            applyHunger(player);
        }

        resetPreviousStats();
    }

    private void syncExperience() {
        var expPlayers = getLiveSharedExperiencePlayers().toList();

        for (ServerPlayer player : expPlayers) {
            var experienceLevelChange = player.experienceLevel - this.previousExperienceLevel;
            this.setExperienceLevels(Math.max(0, this.experienceLevel + experienceLevelChange));
        }

        for (ServerPlayer player : expPlayers) {
            player.giveExperienceLevels(this.experienceLevel - player.experienceLevel);
        }

        this.previousExperienceLevel = this.experienceLevel;
    }

    public void hurtBy(@Nullable ServerPlayer hurtPlayer, @NotNull DamageSource source, float amount) {
        if (amount <= 0) return;
        if (source.is(ModDamageTypes.SHARED_LIFE)) return;
        if (hurtPlayer != null && !Players.isSharedHealthEnabled(hurtPlayer)) return;

        this.setHealth(Math.max(0, getHealth() - amount));

        geSharedHealthPlayers().forEach(player -> {
            sendDamageMessage(player, hurtPlayer, source, amount);

            if (player != hurtPlayer) {
                // TODO: Fix bug: the below player dies first in the message order
                // TODO: Check if this is problematic with invulnerability frames
                player.hurt(damageSource, amount);
            }
        });
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        hurtBy(null, source, amount);
        return true;
    }

    public void healBy(@Nullable ServerPlayer healedPlayer, float healAmount) {
        if (healAmount <= 0) return;
        if (isDeadOrDying()) return;
        if (healedPlayer != null && !Players.isSharedHealthEnabled(healedPlayer)) return;

        this.setHealth(getHealth() + healAmount);

        getLiveSharedHealthPlayers().forEach(player -> {
            if (player != healedPlayer) {
                player.setHealth(getHealth());
                player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
            }
        });
    }

    @Override
    public void heal(float healAmount) {
        healBy(null, healAmount);
    }

    /**
     * Ends the shared life due to the given player's death.
     */
    public void killBy(@Nullable ServerPlayer deadPlayer) {
        if (isDeadOrDying()) return;
        if (deadPlayer != null && !Players.isSharedHealthEnabled(deadPlayer)) return;

        this.setHealth(0);
        LOG.debug("{} has caused shared life death.", deadPlayer != null ? deadPlayer.getName().getString() : "Unknown");

        getLiveSharedHealthPlayers().forEach(player -> {
            if (player != deadPlayer) {
                killPlayer(player);
            }
        });
    }

    @Override
    public void kill() {
        killBy(null);
    }

    private void killPlayer(@NotNull ServerPlayer player) {
        player.hurt(damageSource, Float.MAX_VALUE);
    }

    /**
     * Sends a message to show who took damage.
     */
    private void sendDamageMessage(@NotNull ServerPlayer toPlayer, @Nullable Entity sourceEntity, @NotNull DamageSource source, float damage) {
        var name = sourceEntity != null ? sourceEntity.getName().getString() : NAME;
        var formattedHearts = HEARTS_DECIMAL_FORMAT.format(damage / 2) + " ❤";
        var damageSourceEntity = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        var damageSourceName = damageSourceEntity != null ? damageSourceEntity.getName().getString() : source.getMsgId();

        var message = Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(Component.literal(name).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.took", "took"))
                .append(Component.literal(" "))
                .append(Component.literal(formattedHearts).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.damage", "damage"))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.from", "from"))
                .append(Component.literal(" "))
                .append(Component.literal(damageSourceName).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));

        LOG.debug(message.getString());
        toPlayer.sendSystemMessage(message, false);
    }

    private Stream<ServerPlayer> geSharedHealthPlayers() {
        return this.server.getPlayerList().getPlayers().stream().filter(Players::isSharedHealthEnabled);
    }
    private Stream<ServerPlayer> getLiveSharedHealthPlayers() {
        return this.server.getPlayerList().getPlayers().stream()
                .filter(player -> Players.isSharedHealthEnabled(player) && !player.isDeadOrDying());
    }
    private Stream<ServerPlayer> getLiveSharedHungerPlayers() {
        return this.server.getPlayerList().getPlayers().stream()
                .filter(player -> Players.isSharedHungerEnabled(player) && !player.isDeadOrDying());
    }
    private Stream<ServerPlayer> getLiveSharedExperiencePlayers() {
        return this.server.getPlayerList().getPlayers().stream()
                .filter(player -> Players.isSharedExperienceEnabled(player) && !player.isDeadOrDying());
    }

    @Override
    public @NotNull String toString() {
        return "SharedLife(health=%s, foodLevel=%s, saturation=%s, exhaustion=%s, experienceLevel=%s)"
                .formatted(getHealth(), foodData.getFoodLevel(), foodData.getSaturationLevel(), foodData.getExhaustionLevel(), experienceLevel);
    }
}
