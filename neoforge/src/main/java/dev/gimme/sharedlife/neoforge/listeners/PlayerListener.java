package dev.gimme.sharedlife.neoforge.listeners;

import dev.gimme.sharedlife.application.PlayerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerListener {

    private final PlayerHandler playerHandler;

    public PlayerListener(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @SubscribeEvent
    public void onJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerJoinLevel(player);
    }

    @SubscribeEvent
    public void onChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerChangeGameMode(player, event.getNewGameMode());
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerDamage(player, event.getSource(), event.getNewDamage());
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerHeal(player, event.getAmount());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerDeath(player);
    }
}
