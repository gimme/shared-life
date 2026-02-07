package dev.gimme.sharedlife;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.infrastructure.NightServerConfig;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class Main {

    public static Main INSTANCE;

    public static Main init(MinecraftServer server, Path configDir) {
        if (INSTANCE != null) throw new IllegalStateException("Main has already been initialized");
        INSTANCE = new Main(server, configDir);
        return INSTANCE;
    }

    private final ServerConfig serverConfig;
    private final PlayerHandler playerHandler;
    private final ServerHandler serverHandler;

    private Main(MinecraftServer server, Path configDir) {
        NightServerConfig.SPEC.init(configDir, Constants.MOD_ID + "-server.toml");
        this.serverConfig = new NightServerConfig();

        SharedLife sharedLife = new SharedLife(server);
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
