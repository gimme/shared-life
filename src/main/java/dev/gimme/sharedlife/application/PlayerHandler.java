package dev.gimme.sharedlife.application;

import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.domain.SharedLife;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.text.DecimalFormat;

public class PlayerHandler {

    private static final Logger LOG = LogUtils.getLogger();
    private static final DecimalFormat HEARTS_DECIMAL_FORMAT = new DecimalFormat("0.0");

    private final SharedLife sharedLife;

    public PlayerHandler(SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    public void onPlayerTick(@NotNull ServerPlayer player) {
        sharedLife.applyChangesFrom(player);
    }

    public void onPlayerJoinLevel(@NotNull ServerPlayer player) {
        sharedLife.includeNewPlayer(player);
    }

    public void onPlayerChangeGameMode(@NotNull ServerPlayer player) {
        sharedLife.includeNewPlayer(player);
    }

    public void onPlayerDamage(@NotNull ServerPlayer player, float damage) {
        indicateDamageToOtherPlayers(player);
        broadcastDamageMessage(player, damage);
    }

    public void onPlayerDeath(@NotNull ServerPlayer entity, @Nullable DamageSource source) {
        if (SharedLife.isSharedLifeDeath(source)) return;

        sharedLife.kill();
        LOG.info("{} has caused shared life death.", entity.getName().getString());
    }

    /**
     * Plays the damage effect on all players other than the originally damaged player.
     */
    private void indicateDamageToOtherPlayers(@NotNull Entity sourceEntity) {
        var server = sourceEntity.getServer();
        if (server == null) return;
        var playerList = server.getPlayerList();

        for (var player : playerList.getPlayers()) {
            if (player != sourceEntity) {
                var xDistance = player.getX() - sourceEntity.getX();
                var zDistance = player.getZ() - sourceEntity.getZ();

                player.indicateDamage(xDistance, zDistance);
            }
        }
    }

    /**
     * Broadcasts a message to show who took damage.
     */
    private void broadcastDamageMessage(@NotNull Entity sourceEntity, float damage) {
        var server = sourceEntity.getServer();
        if (server == null) return;
        var playerList = server.getPlayerList();

        var sourceName = sourceEntity.getName().getString();
        var formattedDamage = HEARTS_DECIMAL_FORMAT.format(damage / 2);
        var message = Component.empty()
                .append(Component.literal(sourceName).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.took", "took"))
                .append(Component.literal(" "))
                .append(Component.literal(formattedDamage + " ❤").withStyle(Style.EMPTY.withColor(ChatFormatting.RED)))
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.damage", "damage"))
                .append(Component.literal("!"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        playerList.broadcastSystemMessage(message, false);
    }
}
