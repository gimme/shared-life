package dev.gimme.sharedlife.forge;

import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.config.PlayerSyncStatusChecker;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.forge.listeners.PlayerListener;
import dev.gimme.sharedlife.forge.listeners.ServerListener;
import dev.gimme.sharedlife.forge.plugins.ForgeThirstPlugin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ForgeMod.ID)
public class ForgeMod {

    public static final String ID = "sharedlife";

    public ForgeMod(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC, ID + "-server.toml");
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        var config = new ForgeConfig();
        var sharedLife = new SharedLife(new PlayerSyncStatusChecker(config), new ForgeThirstPlugin());

        MinecraftForge.EVENT_BUS.register(new ServerListener(new ServerHandler(sharedLife)));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(sharedLife)));
    }
}
