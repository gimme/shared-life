package dev.gimme.sharedlife;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.listeners.PlayerListener;
import dev.gimme.sharedlife.listeners.ServerListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeServerConfig.SPEC, Constants.MOD_ID + "-server.toml");
        ServerConfig.INSTANCE = new NeoForgeServerConfig();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var sharedLife = new SharedLife(event.getServer().overworld());

        NeoForge.EVENT_BUS.register(new ServerListener(new ServerHandler(sharedLife)));
        NeoForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(sharedLife)));
    }
}
