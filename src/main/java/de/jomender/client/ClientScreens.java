
package de.jomender.client;

import de.jomender.Jomender;
import de.jomender.ModMenuTypes;
import de.jomender.client.screen.CoalGeneratorScreen;
import de.jomender.client.screen.ElectricSmelterScreen;
import de.jomender.client.screen.GermaniumMinerScreen;
import de.jomender.misc.GeneratorItemTooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Jomender.MOD_ID, dist = Dist.CLIENT)
public class ClientScreens {

    public ClientScreens(IEventBus modBus) {
        modBus.addListener(ClientScreens::register);

        NeoForge.EVENT_BUS.register(GeneratorItemTooltip.class);
    }

    public static void register(RegisterMenuScreensEvent event) {

        event.register(
                ModMenuTypes.COAL_GENERATOR.get(),
                CoalGeneratorScreen::new
        );

        event.register(
                ModMenuTypes.ELECTRIC_SMELTER.get(),
                ElectricSmelterScreen::new
        );

        event.register(
                ModMenuTypes.GERMANIUM_MINER.get(),
                GermaniumMinerScreen::new
        );
    }
}
