package dev.gimme.sharedlife.domain;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.domain.util.ExtractingValueOutput;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.domain.util.Players;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
    private int experienceLevel;

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
        public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
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

        resetPreviousStats();
        LOG.debug("Initialized shared life from player {}: {}", player.getName().getString(), this);
    }

    private void resetPreviousStats() {
        previousFoodLevel = foodData.getFoodLevel();
        previousSaturation = foodData.getSaturationLevel();
        previousExperienceLevel = experienceLevel;
    }

    public void tick() {
        tickHunger();
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

        geSharedHealthPlayers().forEach(player -> sendDamageMessage(player, hurtPlayer, source, amount));

        if (source.is(DamageTypes.STARVE)) {
            // Starvation is the one damage type that applies to all players individually,
            // other damage is applied in the tick method.
            getLiveSharedHealthPlayers().forEach(player -> player.hurtServer(player.level(), damageSource, amount));
        }
    }

    public void healByPlayer(@Nullable ServerPlayer healedPlayer, float healAmount) {
        if (healAmount <= 0) return;
        if (isDead()) return;
        if (healedPlayer != null && !Players.isSharedHealthEnabled(healedPlayer)) return;

        setHealth(getHealth() + healAmount);
    }

    /**
     * Ends the shared life due to the given player's death.
     */
    public void killBy(@NotNull ServerPlayer deadPlayer) {
        if (isDead()) return;
        if (!Players.isSharedHealthEnabled(deadPlayer)) return;

        setHealth(0);
        LOG.debug("{} has caused shared life death.", deadPlayer.getName().getString());
    }

    private void killPlayer(@NotNull ServerPlayer player) {
        player.hurtServer(player.level(), damageSource, Float.MAX_VALUE);
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

    private static final ExtractingValueOutput EXTRACTING_VALUE_OUTPUT = new ExtractingValueOutput();
    private static float getExhaustionLevel(FoodData foodData) {
        foodData.addAdditionalSaveData(EXTRACTING_VALUE_OUTPUT);
        return EXTRACTING_VALUE_OUTPUT.getExhaustion();
    }
    private static void resetExhaustionLevel(FoodData foodData) {
        float exhaustion = getExhaustionLevel(foodData);
        foodData.addExhaustion(-exhaustion);
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
