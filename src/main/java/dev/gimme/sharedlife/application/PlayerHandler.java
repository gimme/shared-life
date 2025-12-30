package dev.gimme.sharedlife.application;

import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.PlayerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class PlayerHandler {

    private static final DecimalFormat HEARTS_DECIMAL_FORMAT = new DecimalFormat("0.0");

    private final SharedLife sharedLife;
    private final PlayerConfig playerConfig;

    public PlayerHandler(SharedLife sharedLife, PlayerConfig playerConfig) {
        this.sharedLife = sharedLife;
        this.playerConfig = playerConfig;
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

    public void onPlayerDamage(@NotNull ServerPlayer player, float damage, float absorbedDamage) {
        indicateDamageToOtherPlayers(player);
        broadcastDamageMessage(player, damage, absorbedDamage);
    }

    public void onPlayerDeath(@NotNull ServerPlayer player) {
        sharedLife.endIt(player);
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
    private void broadcastDamageMessage(@NotNull Entity sourceEntity, float damage, float absorbedDamage) {
        var server = sourceEntity.getServer();
        if (server == null) return;
        var playerList = server.getPlayerList();

        var sourceNameComponent = Component.literal(sourceEntity.getName().getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));

        var damageComponent = Component.empty();
        if (damage > 0 || absorbedDamage == 0) {
            damageComponent.append(Component.literal(" "));
            damageComponent.append(
                    Component.literal(HEARTS_DECIMAL_FORMAT.format(damage / 2) + " ❤")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED))
            );
        }
        if (absorbedDamage > 0) {
            damageComponent.append(Component.literal(" "));
            damageComponent.append(
                    Component.literal(HEARTS_DECIMAL_FORMAT.format(absorbedDamage / 2) + " ❤")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
            );
        }

        var message = Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(sourceNameComponent)
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.took", "took"))
                .append(damageComponent)
                .append(Component.literal(" "))
                .append(Component.translatableWithFallback("message.sharedlife.damage", "damage"))
                .append(Component.literal("!"));
        playerList.broadcastSystemMessage(message, false);
    }
}
