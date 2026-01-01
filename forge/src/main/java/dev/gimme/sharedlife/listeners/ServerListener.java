package dev.gimme.sharedlife.listeners;

import dev.gimme.sharedlife.application.ServerHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerListener {

    private final ServerHandler serverHandler;

    public ServerListener(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverHandler.onServerTick();
    }
}
