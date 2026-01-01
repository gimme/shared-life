package dev.gimme.sharedlife.listeners;

import dev.gimme.sharedlife.application.PlayerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        playerHandler.onPlayerChangeGameMode(player);
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerDamage(player, event.getSource(), event.getAmount());
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
