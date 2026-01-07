package dev.gimme.sharedlife;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.listeners.PlayerListener;
import dev.gimme.sharedlife.listeners.ServerListener;
import dev.gimme.sharedlife.plugins.ForgeThirstPlugin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class ForgeMod {

    public ForgeMod(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, ForgeServerConfig.SPEC, Constants.MOD_ID + "-server.toml");
        ServerConfig.INSTANCE = new ForgeServerConfig();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var sharedLife = new SharedLife(event.getServer(), new ForgeThirstPlugin());

        MinecraftForge.EVENT_BUS.register(new ServerListener(new ServerHandler(sharedLife)));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(sharedLife)));
    }
}
