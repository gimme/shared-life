package dev.gimme.sharedlife.domain;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class SharedLife {

    private static final DecimalFormat HEARTS_DECIMAL_FORMAT = new DecimalFormat("0.0");

    private static final Logger LOG = LogUtils.getLogger();

    private final MinecraftServer server;
    private final ServerConfig config;
    private final DamageSource damageSource;
    private final Heart heart;

    private final FoodData foodData = new FoodData();

    /**
     * Runs vanilla's natural regeneration once for the whole group (see {@link #tickCombinedNaturalRegen}).
     * Its food and saturation are rebuilt from the group's minimums every tick; the only state it carries
     * across ticks is vanilla's internal regeneration timer, which restarts with each new shared life
     * (see {@link #initializeFrom}) just as a respawned vanilla player's does.
     */
    private FoodData combinedRegenFoodData = new FoodData();

    private int experienceLevel;

    private final Map<String, Float> damageTakenSinceFullHealth = new LinkedHashMap<>();

    private int previousFoodLevel;
    private float previousSaturation;
    private int previousExperienceLevel;

    public SharedLife(@NotNull MinecraftServer server, @NotNull ServerConfig config) {
        this.server = server;
        this.config = config;
        this.damageSource = new DamageSource(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModDamageTypes.SHARED_LIFE));
        this.heart = new Heart(server);
    }

    public class Heart extends FakePlayer {
        public Heart(@NotNull MinecraftServer server) {
            super(server.overworld(), new GameProfile(UUID.randomUUID(), Constants.MOD_NAME));
            setHealth(0);
        }

        @Override
        public boolean hurt(@NotNull DamageSource source, float amount) {
            hurtByPlayer(null, source, amount);
            return true;
        }

        @Override
        public void heal(float healAmount) {
            healByPlayer(null, healAmount);
        }
    }

    /**
     * Syncs up a potential new player into the shared life.
     */
    public void includePotentialNewPlayer(ServerPlayer player) {
        if (player.isDeadOrDying()) return;
        if (Players.isEthereal(player)) return;

        if (isDead()) {
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

            if (isDead()) {
                killPlayer(player);
            }
        }

        if (Players.isSharedHungerEnabled(player)) {
            applyHunger(player);
        }

        if (Players.isSharedExperienceEnabled(player)) {
            player.setExperienceLevels(experienceLevel);
        }
    }

    private void applyHunger(ServerPlayer player) {
        player.getFoodData().setFoodLevel(foodData.getFoodLevel());
        player.getFoodData().setSaturation(foodData.getSaturationLevel());
    }

    /**
     * Initializes the shared life with the state of the given player.
     */
    private void initializeFrom(ServerPlayer player) {
        setHealth(player.getHealth());
        setExperienceLevels(player.experienceLevel);

        foodData.setFoodLevel(player.getFoodData().getFoodLevel());
        foodData.setSaturation(player.getFoodData().getSaturationLevel());
        foodData.addExhaustion(getExhaustionLevel(player.getFoodData()));
        resetExhaustionLevel(player.getFoodData());

        combinedRegenFoodData = new FoodData(); // fresh life, fresh vanilla regeneration timer

        damageTakenSinceFullHealth.clear();
        resetPreviousStats();
        LOG.debug("Initialized shared life from player {}: {}", player.getName().getString(), this);
    }

    private void resetPreviousStats() {
        previousFoodLevel = foodData.getFoodLevel();
        previousSaturation = foodData.getSaturationLevel();
        previousExperienceLevel = experienceLevel;
    }

    /**
     * Writes the shared life state to the given tag, to be saved with the world (see
     * {@code SharedLifePersistence}). Deliberately minimal: the combined-regen timer and the
     * damage-summary ledger are transient and restart empty.
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putFloat("health", getHealth());
        tag.putInt("experienceLevel", experienceLevel);

        var foodTag = new CompoundTag();
        foodData.addAdditionalSaveData(foodTag);
        tag.put("food", foodTag);

        return tag;
    }

    /**
     * Restores the shared life state saved with the world, replacing the current state.
     */
    public void load(CompoundTag tag) {
        setHealth(tag.getFloat("health"));
        setExperienceLevels(tag.getInt("experienceLevel"));
        foodData.readAdditionalSaveData(tag.getCompound("food"));

        resetPreviousStats();
        LOG.debug("Loaded shared life saved with the world: {}", this);
    }

    public void tick() {
        tickHunger();
        tickCombinedNaturalRegen();
        syncHealth();
        syncExperience();
    }

    private void tickHunger() {
        var hungryPlayers = getLiveSharedHungerPlayers().toList();
        if (hungryPlayers.isEmpty()) return;

        for (ServerPlayer player : hungryPlayers) {
            var foodLevelChange = player.getFoodData().getFoodLevel() - previousFoodLevel;
            var saturationChange = player.getFoodData().getSaturationLevel() - previousSaturation;
            var exhaustionChange = getExhaustionLevel(player.getFoodData());

            foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() + foodLevelChange));
            foodData.setSaturation(Math.max(0, foodData.getSaturationLevel() + saturationChange));
            foodData.addExhaustion(exhaustionChange);
            resetExhaustionLevel(player.getFoodData());
        }

        foodData.tick(heart);

        for (ServerPlayer player : hungryPlayers) {
            applyHunger(player);
        }

        resetPreviousStats();
    }

    /**
     * Runs natural regeneration once for the whole group instead of once per fed player.
     *
     * <p>Each player's own regeneration is suppressed (see {@code MixinFoodData}); instead, vanilla's own
     * regeneration logic runs here on {@link #combinedRegenFoodData}, primed with the group's lowest food
     * and saturation levels. The group therefore heals at the rate of a single player, only while every player
     * meets the vanilla conditions (everyone fed enough to regenerate; fast regeneration only when
     * everyone is at full food with saturation), and the exhaustion vanilla charges for each heal is paid
     * by every player.
     *
     * <p>Reusing {@link FoodData#tick} instead of re-implementing it keeps the conditions, speed, cost and
     * timer semantics exactly vanilla's — including the {@code naturalRegeneration} gamerule, which the
     * heart's tick still respects.
     */
    private void tickCombinedNaturalRegen() {
        if (!config.shareHealth() || config.shareHunger() || !config.combineNaturalRegeneration()) return;

        var players = getLiveSharedHealthPlayers().toList();
        if (players.isEmpty()) return;

        var foodLevel = Integer.MAX_VALUE;
        var saturation = Float.MAX_VALUE;
        for (ServerPlayer player : players) {
            foodLevel = Math.min(foodLevel, player.getFoodData().getFoodLevel());
            saturation = Math.min(saturation, player.getFoodData().getSaturationLevel());
        }

        // Clamped above zero so this FoodData never runs vanilla's starvation branch: a starving player
        // already starves through their own FoodData, and that damage reaches the group as shared damage.
        combinedRegenFoodData.setFoodLevel(Math.max(1, foodLevel));
        combinedRegenFoodData.setSaturation(saturation);
        combinedRegenFoodData.tick(heart); // any heal lands on the shared health via Heart#heal

        var exhaustion = getExhaustionLevel(combinedRegenFoodData);
        if (exhaustion > 0) {
            resetExhaustionLevel(combinedRegenFoodData);
            players.forEach(player -> player.getFoodData().addExhaustion(exhaustion));
        }
    }

    private void syncHealth() {
        getLiveSharedHealthPlayers().forEach(player -> {
            var healthChange = getHealth() - player.getHealth();
            if (healthChange == 0) return;

            if (getHealth() <= 0) {
                killPlayer(player);
                return;
            }

            player.setHealth(getHealth());
            player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));

            if (healthChange < 0) {
                notifyHurt(player);
            }
        });
    }

    private void syncExperience() {
        var expPlayers = getLiveSharedExperiencePlayers().toList();

        for (ServerPlayer player : expPlayers) {
            var experienceLevelChange = player.experienceLevel - previousExperienceLevel;
            setExperienceLevels(Math.max(0, experienceLevel + experienceLevelChange));
        }

        for (ServerPlayer player : expPlayers) {
            player.giveExperienceLevels(experienceLevel - player.experienceLevel);
        }

        previousExperienceLevel = experienceLevel;
    }

    public void hurtByPlayer(@Nullable ServerPlayer hurtPlayer, @NotNull DamageSource source, float amount) {
        if (amount <= 0) return;
        if (source.is(ModDamageTypes.SHARED_LIFE)) return;
        if (hurtPlayer != null && !Players.isSharedHealthEnabled(hurtPlayer)) return;

        setHealth(getHealth() - amount);

        var name = hurtPlayer != null ? hurtPlayer.getName().getString() : Constants.MOD_NAME;
        damageTakenSinceFullHealth.merge(name, amount, Float::sum);

        geSharedHealthPlayers().forEach(player -> sendDamageMessage(player, hurtPlayer, source, amount));

        if (source.is(DamageTypes.STARVE)) {
            // Starvation is the one damage type that applies to all players individually,
            // other damage is applied in the tick method.
            getLiveSharedHealthPlayers().forEach(player -> player.hurt(damageSource, amount));
        }
    }

    public void healByPlayer(@Nullable ServerPlayer healedPlayer, float healAmount) {
        if (healAmount <= 0) return;
        if (isDead()) return;
        if (healedPlayer != null && !Players.isSharedHealthEnabled(healedPlayer)) return;

        setHealth(getHealth() + healAmount);

        if (getHealth() >= heart.getMaxHealth()) {
            // Fully healed: the next death summary should only cover the fall from here.
            damageTakenSinceFullHealth.clear();
        }
    }

    /**
     * Restores the shared life after the given player is saved from death by a totem of undying.
     *
     * By design, this only fires for the player who personally takes the fatal blow while holding a
     * totem.
     */
    public void protectByTotem(@NotNull ServerPlayer player) {
        if (!Players.isSharedHealthEnabled(player)) return;
        if (getHealth() < 1) {
            setHealth(1);
        }
        LOG.debug("{} used a totem of undying, reviving the shared life.", player.getName().getString());
    }

    /**
     * Ends the shared life due to the given player's death.
     */
    public void killBy(@NotNull ServerPlayer deadPlayer) {
        if (isDead()) return;
        if (!Players.isSharedDeathEnabled(deadPlayer)) return;

        setHealth(0);
        announceDeathSummary();
        LOG.debug("{} has caused shared life death.", deadPlayer.getName().getString());
    }

    /**
     * Announces how much damage each player took since the shared health was last full — the fall that ended
     * the life — then starts a fresh count.
     */
    private void announceDeathSummary() {
        var damageEntries = damageTakenSinceFullHealth.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .toList();
        damageTakenSinceFullHealth.clear();

        if (!config.announceDeathSummary()) return;
        if (damageEntries.isEmpty()) return;

        var message = Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(Component.translatableWithFallback("message.sharedlife.damage_taken", "Damage taken"))
                .append(Component.literal(": "));

        for (int i = 0; i < damageEntries.size(); i++) {
            if (i > 0) {
                message.append(Component.literal(", "));
            }
            var entry = damageEntries.get(i);
            message
                    .append(Component.literal(entry.getKey()).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
                    .append(Component.literal(" "))
                    .append(Component.literal(HEARTS_DECIMAL_FORMAT.format(entry.getValue() / 2)).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                    .append(Component.literal("❤").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        }

        geSharedHealthPlayers().forEach(player -> player.sendSystemMessage(message, false));
        LOG.debug(message.getString());
    }

    private void killPlayer(@NotNull ServerPlayer player) {
        player.hurt(damageSource, Float.MAX_VALUE);
    }

    /**
     * Sends a message to show who took damage.
     */
    private void sendDamageMessage(@NotNull ServerPlayer toPlayer, @Nullable Entity sourceEntity, @NotNull DamageSource source, float damage) {
        var name = sourceEntity != null ? sourceEntity.getName().getString() : Constants.MOD_NAME;
        var formattedHearts = HEARTS_DECIMAL_FORMAT.format(damage / 2);
        var damageSourceEntity = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        var damageSourceName = damageSourceEntity != null ? damageSourceEntity.getName().getString() : source.getMsgId();

        var message = Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(Component.literal(name).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.took", "took"))
                .append(Component.literal(" "))
                .append(Component.literal(formattedHearts).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                .append(Component.literal(" "))
                .append(Component.literal("❤").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.damage", "damage"));

        if (config.includeDamageSource()) {
            message
                    .append(Component.literal(" "))
                    .append(Component.translatableWithFallback("message.sharedlife.from", "from"))
                    .append(Component.literal(" "))
                    .append(Component.literal(damageSourceName).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(true)));
        }

        if (config.announceDamage()) {
            toPlayer.sendSystemMessage(message, false);
        }
        LOG.debug(message.getString());
    }

    private Stream<ServerPlayer> geSharedHealthPlayers() {
        return server.getPlayerList().getPlayers().stream().filter(Players::isSharedHealthEnabled);
    }
    private Stream<ServerPlayer> getLiveSharedHealthPlayers() {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isDeadOrDying() && Players.isSharedHealthEnabled(player));
    }
    private Stream<ServerPlayer> getLiveSharedHungerPlayers() {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isDeadOrDying() && Players.isSharedHungerEnabled(player));
    }
    private Stream<ServerPlayer> getLiveSharedExperiencePlayers() {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isDeadOrDying() && Players.isSharedExperienceEnabled(player));
    }

    private float getHealth() {
        return heart.getHealth();
    }

    private void setHealth(float health) {
        heart.setHealth(health);
    }

    private boolean isDead() {
        return heart.isDeadOrDying();
    }

    private void setExperienceLevels(int experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    @Override
    public @NotNull String toString() {
        return "SharedLife(health=%s, foodLevel=%s, saturation=%s, exhaustion=%s, experienceLevel=%s)"
                .formatted(getHealth(), foodData.getFoodLevel(), foodData.getSaturationLevel(), getExhaustionLevel(foodData), experienceLevel);
    }

    private static float getExhaustionLevel(FoodData foodData) {
        return foodData.getExhaustionLevel();
    }
    private static void resetExhaustionLevel(FoodData foodData) {
        foodData.setExhaustion(0);
    }

    /**
     * Notifies the given player that they have been hurt visually and audibly.
     */
    private static void notifyHurt(ServerPlayer player) {
        var volume = 0.5f;
        var pitch = 0.8f;

        player.connection.send(new ClientboundHurtAnimationPacket(player));
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_HURT), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), volume, pitch, player.level().random.nextLong()));
    }
}
