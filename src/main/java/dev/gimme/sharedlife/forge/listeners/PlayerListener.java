package dev.gimme.sharedlife.forge.listeners;

import dev.gimme.sharedlife.application.PlayerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
        if (!(event.player instanceof ServerPlayer player)) return;
        playerHandler.onPlayerTick(player);
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
        playerHandler.onPlayerDamage(player, event.getAmount());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerHandler.onPlayerDeath(player);
    }
}
