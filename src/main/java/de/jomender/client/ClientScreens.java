
package de.jomender.client;

import de.jomender.Jomender;
import de.jomender.ModMenuTypes;
import de.jomender.ModBlockEntities;
import de.jomender.client.render.ArcFusionChamberRenderer;
import de.jomender.client.render.ElectricSmelterCoreRenderer;
import de.jomender.client.render.GermaniumMinerRangeRenderer;
import de.jomender.client.render.ReactorWaterRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import de.jomender.client.screen.ArcFusionChamberScreen;
import de.jomender.client.screen.CoalGeneratorScreen;
import de.jomender.client.screen.ElectricSmelterScreen;
import de.jomender.client.screen.GermaniumMinerScreen;
import de.jomender.client.screen.ReactorScreen;
import de.jomender.misc.GeneratorItemTooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Jomender.MOD_ID, dist = Dist.CLIENT)
public class ClientScreens {

    public ClientScreens(IEventBus modBus) {
        modBus.addListener(ClientScreens::register);
        modBus.addListener(ClientScreens::registerRenderers);

        NeoForge.EVENT_BUS.register(GeneratorItemTooltip.class);
        // Shift-Infos aus ItemStyles
        NeoForge.EVENT_BUS.addListener(ItemStyleTooltips::onTooltip);
        // Upgrades der Endarium-Rüstung im Tooltip
        NeoForge.EVENT_BUS.addListener(ArmorUpgradeTooltip::onTooltip);
        // Rezepte für JEI merken - vor JEI selbst (HIGHEST)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientRecipeCache::onRecipesReceived);

    }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.GERMANIUM_MINER.get(),
                GermaniumMinerRangeRenderer::new
        );
        event.registerBlockEntityRenderer(
                ModBlockEntities.ELECTRIC_SMELTER.get(),
                ElectricSmelterCoreRenderer::new
        );
        event.registerBlockEntityRenderer(
                ModBlockEntities.ARC_FUSION_CHAMBER.get(),
                ArcFusionChamberRenderer::new
        );
        // Kühlwasser im Reaktor (sichtbar durchs Reaktor-Glas)
        event.registerBlockEntityRenderer(
                ModBlockEntities.REACTOR_CONTROLLER.get(),
                ReactorWaterRenderer::new
        );
    }

//    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
//        event.registerBlockEntityRenderer(ModBlockEntities.GERMANIUM_MINER.get(),
//                GermaniumMinerRangeRenderer::new);
//    }

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

        event.register(
                ModMenuTypes.ARC_FUSION_CHAMBER.get(),
                ArcFusionChamberScreen::new
        );

        event.register(ModMenuTypes.REACTOR_CONTROLLER.get(), ReactorScreen::new);
    }
}
