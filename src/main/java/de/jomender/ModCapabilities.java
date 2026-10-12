

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
                (generator, side) -> generator.getEnergyHandler(side)
        );

        // =========================
        // COAL GENERATOR: KOHLE PER ROHR
        // =========================

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COAL_GENERATOR.get(),
                (generator, side) -> generator.getItemHandler(side)
        );

        // =========================
        // ARC FUSION CHAMBER: FE NUR VON UNTEN, ITEMS NUR OBEN
        // =========================

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.ARC_FUSION_CHAMBER.get(),
                (chamber, side) -> chamber.getEnergyHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.ARC_FUSION_CHAMBER.get(),
                (chamber, side) -> chamber.getItemHandler(side)
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


// =========================
// GERMANIUM MINER: FE-EINGANG
// =========================

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.GERMANIUM_MINER.get(),
                (miner, side) -> miner.getEnergyStorage()
        );

// =========================
// GERMANIUM MINER: ITEM-AUSGANG
// =========================

        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.GERMANIUM_MINER.get(),
                (miner, side) -> miner.getItemHandler(side)
        );


        // =========================
        // MINER-STRUKTUR: FE-EINGANG
        // =========================

        event.registerBlock(
                Capabilities.Energy.BLOCK,
                (level, pos, state, blockEntity, side) -> {

                    if (!(state.getBlock()
                            instanceof de.jomender.block.GermaniumMinerPartBlock)) {
                        return null;
                    }

                    var controllerPos =
                            de.jomender.block.GermaniumMinerPartBlock
                                    .getControllerPos(pos, state);

                    if (level.getBlockEntity(controllerPos)
                            instanceof de.jomender.blockentity.GermaniumMinerBlockEntity miner) {
                        return miner.getEnergyStorage();
                    }

                    return null;
                },
                ModBlocks.GERMANIUM_MINER_PART.get()
        );

        // =========================
        // MINER-STRUKTUR: ITEM-AUSGANG
        // =========================

        event.registerBlock(
                Capabilities.Item.BLOCK,
                (level, pos, state, blockEntity, side) -> {

                    if (!(state.getBlock()
                            instanceof de.jomender.block.GermaniumMinerPartBlock)) {
                        return null;
                    }

                    var controllerPos =
                            de.jomender.block.GermaniumMinerPartBlock
                                    .getControllerPos(pos, state);

                    if (level.getBlockEntity(controllerPos)
                            instanceof de.jomender.blockentity.GermaniumMinerBlockEntity miner) {
                        return miner.getItemHandler(side);
                    }

                    return null;
                },
                ModBlocks.GERMANIUM_MINER_PART.get()
        );


    
        // =========================
        // REAKTOR-ANSCHLUSS: je nach Modus Items, Wasser oder Strom
        // =========================

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.REACTOR_PORT.get(),
                (port, side) -> port.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.REACTOR_PORT.get(),
                (port, side) -> port.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.REACTOR_PORT.get(),
                (port, side) -> port.getEnergyHandler(side));

        // =========================
        // UNENDLICHES WASSER: alle Seiten, kein Limit
        // =========================

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.INFINITE_WATER.get(),
                (source, side) -> source.getFluidHandler());
    }

}
