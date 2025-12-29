package dev.gimme.sharedlife.application;

import dev.gimme.sharedlife.domain.SharedLife;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerHandler {

    private final SharedLife sharedLife;

    public PlayerHandler(SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    public void onPlayerTick(@NotNull Player player) {
        sharedLife.updateFromPlayer(player);
    }

    public void onPlayerRespawn(Player player) {
        if (sharedLife.isDead()) {
            sharedLife.initialize(player);
        }
    }

    public void onPlayerJoin(Player player) {
        if (!sharedLife.isInitialized()) {
            sharedLife.initialize(player);
        } else {
            sharedLife.syncToPlayer(player);
        }
    }

    public void onDamage(@NotNull Entity sourceEntity, float damage) {
        if (!(sourceEntity instanceof Player player)) return;

        indicateDamageToOtherPlayers(player);
        broadcastDamageMessage(player, damage);
    }

    public void onDeath(@NotNull Entity sourceEntity) {
        if (!(sourceEntity instanceof Player)) return;

        sharedLife.kill();
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
        var formattedDamage = String.format("%.1f", damage);
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
