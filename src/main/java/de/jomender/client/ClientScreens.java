
package de.jomender.client;

import de.jomender.Jomender;
import de.jomender.ModMenuTypes;
import de.jomender.client.screen.CoalGeneratorScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = Jomender.MOD_ID, dist = Dist.CLIENT)
public class ClientScreens {

    public ClientScreens(IEventBus modBus) {
        modBus.addListener(ClientScreens::register);
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.COAL_GENERATOR.get(),
                CoalGeneratorScreen::new
        );
    }
}
