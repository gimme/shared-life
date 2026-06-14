package dev.gimme.sharedlife.fabric;

import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.infrastructure.FcapServerConfig;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.neoforged.fml.config.ModConfig;

public class FabricMod implements ModInitializer {

    @Override
    public void onInitialize() {
        NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);

        ServerLifecycleEvents.SERVER_STARTED.register(Main::init);
        ServerTickEvents.END_SERVER_TICK.register(server -> Main.INSTANCE.getServerHandler().onServerTick());
    }
}
