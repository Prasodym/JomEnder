
package de.jomender;

import de.jomender.blockentity.CoalGeneratorBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
