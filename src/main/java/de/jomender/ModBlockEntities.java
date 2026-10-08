
package de.jomender;

import de.jomender.blockentity.CoalGeneratorBlockEntity;
import de.jomender.blockentity.ElectricSmelterBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import de.jomender.blockentity.GermaniumMinerBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

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



    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
