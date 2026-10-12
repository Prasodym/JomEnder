
package de.jomender;

import de.jomender.blockentity.ArcFusionChamberBlockEntity;
import de.jomender.blockentity.CoalGeneratorBlockEntity;
import de.jomender.blockentity.ElectricSmelterBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import de.jomender.blockentity.GermaniumMinerBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import de.jomender.blockentity.InfiniteWaterBlockEntity;
import de.jomender.reactor.ReactorControllerBlockEntity;
import de.jomender.reactor.ReactorPortBlockEntity;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE,
            Jomender.MOD_ID
    );

    public static final Supplier<BlockEntityType<CoalGeneratorBlockEntity>>
            COAL_GENERATOR = BLOCK_ENTITIES.register(
            "coal_generator",
            () -> new BlockEntityType<>(
                    CoalGeneratorBlockEntity::new,
                    false,
                    ModBlocks.COAL_GENERATOR.get()
            )
    );


    public static final Supplier<BlockEntityType<ArcFusionChamberBlockEntity>>
            ARC_FUSION_CHAMBER = BLOCK_ENTITIES.register(
            "arc_fusion_chamber",
            () -> new BlockEntityType<>(
                    ArcFusionChamberBlockEntity::new,
                    false,
                    ModBlocks.ARC_FUSION_CHAMBER.get()
            )
    );

    public static final Supplier<BlockEntityType<ElectricSmelterBlockEntity>>
            ELECTRIC_SMELTER = BLOCK_ENTITIES.register(
            "electric_smelter",
            () -> new BlockEntityType<>(
                    ElectricSmelterBlockEntity::new,
                    false,
                    ModBlocks.ELECTRIC_SMELTER.get()
            )
    );


    public static final Supplier<BlockEntityType<GermaniumMinerBlockEntity>>
            GERMANIUM_MINER = BLOCK_ENTITIES.register(
            "germanium_miner",
            () -> new BlockEntityType<>(
                    GermaniumMinerBlockEntity::new,
                    false,
                    ModBlocks.GERMANIUM_MINER.get()
            )
    );



    public static final Supplier<BlockEntityType<ReactorControllerBlockEntity>>
            REACTOR_CONTROLLER = BLOCK_ENTITIES.register("reactor_controller",
            () -> new BlockEntityType<>(ReactorControllerBlockEntity::new, false, ModBlocks.REACTOR_CONTROLLER.get()));

    public static final Supplier<BlockEntityType<ReactorPortBlockEntity>>
            REACTOR_PORT = BLOCK_ENTITIES.register("reactor_port",
            () -> new BlockEntityType<>(ReactorPortBlockEntity::new, false, ModBlocks.REACTOR_PORT.get()));

    public static final Supplier<BlockEntityType<InfiniteWaterBlockEntity>>
            INFINITE_WATER = BLOCK_ENTITIES.register("infinite_water",
            () -> new BlockEntityType<>(InfiniteWaterBlockEntity::new, false, ModBlocks.INFINITE_WATER.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
