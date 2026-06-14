package dev.gimme.sharedlife.neoforge;

import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.infrastructure.FcapServerConfig;
import dev.gimme.sharedlife.neoforge.listeners.PlayerListener;
import dev.gimme.sharedlife.neoforge.listeners.ServerListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var main = Main.init(event.getServer());

        NeoForge.EVENT_BUS.register(new ServerListener(main.getServerHandler()));
        NeoForge.EVENT_BUS.register(new PlayerListener(main.getPlayerHandler()));
    }
}
