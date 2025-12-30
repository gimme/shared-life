package dev.gimme.sharedlife.neoforge.listeners;

import dev.gimme.sharedlife.application.ServerHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerListener {

    private final ServerHandler serverHandler;

    public ServerListener(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        serverHandler.onServerTick(event.getServer());
    }
}
