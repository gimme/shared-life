package dev.gimme.sharedlife.neoforge;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.PlayerSyncStatusChecker;
import dev.gimme.sharedlife.neoforge.listeners.PlayerListener;
import dev.gimme.sharedlife.neoforge.listeners.ServerListener;
import dev.gimme.sharedlife.neoforge.plugins.NeoForgeThirstPlugin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(NeoForgeMod.ID)
public class NeoForgeMod {

    public static final String ID = "sharedlife";

    public NeoForgeMod(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC, ID + "-server.toml");
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        var config = new NeoForgeConfig();
        var sharedLife = new SharedLife(new PlayerSyncStatusChecker(config), new NeoForgeThirstPlugin());

        NeoForge.EVENT_BUS.register(new ServerListener(new ServerHandler(sharedLife)));
        NeoForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(sharedLife)));
    }
}
