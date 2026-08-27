package dev.gimme.sharedlife.neoforge.listeners;

import dev.gimme.sharedlife.Main;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public class PlayerListener {

    @SubscribeEvent
    public void onJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(player);
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerHeal(player, event.getAmount());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerDeath(player);
    }
}
