package dev.gimme.sharedlife;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.infrastructure.FcapServerConfig;
import dev.gimme.sharedlife.infrastructure.SharedLifePersistence;
import net.minecraft.server.MinecraftServer;

public class Main {

    public static Main INSTANCE;

    public static Main init(MinecraftServer server) {
        INSTANCE = new Main(server);
        return INSTANCE;
    }

    private final ServerConfig serverConfig;
    private final PlayerHandler playerHandler;
    private final ServerHandler serverHandler;

    private Main(MinecraftServer server) {
        this.serverConfig = new FcapServerConfig();

        SharedLife sharedLife = new SharedLife(server, serverConfig);
        SharedLifePersistence.attach(sharedLife, server);
        this.playerHandler = new PlayerHandler(sharedLife);
        this.serverHandler = new ServerHandler(sharedLife);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }
}
