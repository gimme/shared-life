package dev.gimme.sharedlife.forge;

import com.mojang.logging.LogUtils;
import dev.gimme.sharedlife.application.PlayerHandler;
import dev.gimme.sharedlife.application.ServerHandler;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.forge.listeners.PlayerListener;
import dev.gimme.sharedlife.forge.listeners.ServerListener;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod("sharedlife")
public class ForgeMod {

    private static final Logger LOG = LogUtils.getLogger();

    public ForgeMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.register(Config.class);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOG.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOG.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOG.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOG.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        var sharedLife = new SharedLife();

        MinecraftForge.EVENT_BUS.register(new ServerListener(new ServerHandler(sharedLife)));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(new PlayerHandler(sharedLife)));
    }
}
