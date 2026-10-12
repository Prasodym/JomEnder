
package de.jomender;

import de.jomender.menu.ArcFusionChamberMenu;
import de.jomender.menu.CoalGeneratorMenu;
import de.jomender.menu.ElectricSmelterMenu;
import de.jomender.menu.GermaniumMinerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import de.jomender.reactor.ReactorMenu;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    Jomender.MOD_ID
            );

    public static final Supplier<MenuType<CoalGeneratorMenu>>
            COAL_GENERATOR = MENUS.register(
            "coal_generator",
            // IMenuTypeExtension: der Client-Konstruktor bekommt zusätzlich die Blockposition.
            () -> IMenuTypeExtension.create(CoalGeneratorMenu::new)
    );

    public static final Supplier<MenuType<ElectricSmelterMenu>>
            ELECTRIC_SMELTER = MENUS.register(
            "electric_smelter",
            () -> IMenuTypeExtension.create(ElectricSmelterMenu::new)
    );

    public static final Supplier<MenuType<GermaniumMinerMenu>>
            GERMANIUM_MINER = MENUS.register(
            "germanium_miner",
            () -> new MenuType<>(
                    GermaniumMinerMenu::new,
                    FeatureFlags.DEFAULT_FLAGS
            )
    );

    public static final Supplier<MenuType<ArcFusionChamberMenu>>
            ARC_FUSION_CHAMBER = MENUS.register(
            "arc_fusion_chamber",
            () -> IMenuTypeExtension.create(ArcFusionChamberMenu::new)
    );

    public static final Supplier<MenuType<ReactorMenu>>
            REACTOR_CONTROLLER = MENUS.register("reactor_controller",
            () -> IMenuTypeExtension.create(ReactorMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
