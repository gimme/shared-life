package dev.gimme.sharedlife.application;

import dev.gimme.sharedlife.domain.SharedLife;
import net.minecraft.server.MinecraftServer;

public class ServerHandler {

    private final SharedLife sharedLife;

    public ServerHandler(SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    public void onServerTick(MinecraftServer server) {
        sharedLife.tick(server.getPlayerList());
    }
}
