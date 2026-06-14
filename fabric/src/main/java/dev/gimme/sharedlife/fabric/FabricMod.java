package dev.gimme.sharedlife.fabric;

import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.infrastructure.FcapServerConfig;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.neoforged.fml.config.ModConfig;

public class FabricMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);

        // Register server starting event
        ServerLifecycleEvents.SERVER_STARTED.register(mainServer -> {
            var main = Main.init(mainServer);

            // Register server tick event
            ServerTickEvents.END_SERVER_TICK.register(tickServer -> main.getServerHandler().onServerTick());
        });
    }
}
