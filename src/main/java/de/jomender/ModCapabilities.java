

package de.jomender;

import net.minecraft.core.Direction;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {

        // =========================
        // COAL GENERATOR: FE-AUSGANG
        // =========================

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.COAL_GENERATOR.get(),
                (generator, side) -> generator.getEnergyStorage()
        );

        // =========================
        // ELECTRIC SMELTER: FE-EINGANG
        // =========================

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.ELECTRIC_SMELTER.get(),
                (smelter, side) -> smelter.getEnergyStorage()
        );

        // =========================
        // ELECTRIC SMELTER: ITEM-ROHRE
        // =========================

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.ELECTRIC_SMELTER.get(),
                (smelter, side) -> smelter.getItemHandler(side)
        );
    }
}
