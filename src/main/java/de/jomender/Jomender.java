package de.jomender;

import com.mojang.logging.LogUtils;
import de.jomender.Events.ModOreEvents;
import de.jomender.misc.ModCreativeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Jomender.MOD_ID)
public class Jomender {
    public static final String MOD_ID = "jomender";
    private static final Logger LOGGER = LogUtils.getLogger();


public Jomender(IEventBus bus, ModContainer modContainer) {


        // Register the common setup method for modloading
        bus.addListener(this::commonSetup);

        // Register the DeferredRegister to the mod event bus so blocks and items can be registered


        NeoForge.EVENT_BUS.register(this);

        ModCreativeTabs.register(bus);

        ModBlocks.register(bus);
        ModItems.register(bus);

        ModBlockEntities.register(bus);

        ModMenuTypes.register(bus);
        ModDataComponents.register(bus);
        bus.addListener(ModCapabilities::register);

        NeoForge.EVENT_BUS.addListener(ModOreEvents::onBreak);


    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Initializing Jomender Mod");
    }

}
