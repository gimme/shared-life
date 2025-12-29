package dev.gimme.sharedlife.forge.listeners;

import dev.gimme.sharedlife.application.PlayerHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerListener {

    private final PlayerHandler playerHandler;

    public PlayerListener(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        playerHandler.onPlayerTick(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        playerHandler.onPlayerRespawn(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        playerHandler.onPlayerJoin(event.getEntity());
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        playerHandler.onDamage(event.getEntity(), event.getAmount());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        playerHandler.onDeath(event.getEntity());
    }
}
