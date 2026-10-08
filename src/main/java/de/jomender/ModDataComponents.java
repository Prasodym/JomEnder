
package de.jomender;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE,
                    Jomender.MOD_ID
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Integer>
            > GENERATOR_ENERGY = COMPONENTS.registerComponentType(
            "generator_energy",
            builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
