
package de.jomender;

import de.jomender.menu.CoalGeneratorMenu;
import de.jomender.menu.ElectricSmelterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

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
            () -> new MenuType<>(
                    CoalGeneratorMenu::new,
                    FeatureFlags.DEFAULT_FLAGS
            )
    );


    public static final Supplier<MenuType<ElectricSmelterMenu>>
            ELECTRIC_SMELTER = MENUS.register(
            "electric_smelter",
            () -> new MenuType<>(
                    ElectricSmelterMenu::new,
                    FeatureFlags.DEFAULT_FLAGS
            )
    );


    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
