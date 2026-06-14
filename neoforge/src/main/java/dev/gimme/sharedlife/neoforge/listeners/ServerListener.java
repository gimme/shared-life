package dev.gimme.sharedlife.neoforge.listeners;

import dev.gimme.sharedlife.Main;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerListener {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        Main.INSTANCE.getServerHandler().onServerTick();
    }
}
