
package de.jomender.compat.jade;

import de.jomender.blockentity.CoalGeneratorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

public class GeneratorEnergyData
        implements StreamServerDataProvider<BlockAccessor, Integer> {

    public static final GeneratorEnergyData INSTANCE =
            new GeneratorEnergyData();

    @Override
    public @Nullable Integer streamData(BlockAccessor accessor) {
        if (accessor.getBlockEntity()
                instanceof CoalGeneratorBlockEntity generator) {
            return generator.getEnergy();
        }
        return null;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Integer> streamCodec() {
        return ByteBufCodecs.VAR_INT.cast();
    }

    @Override
    public Identifier getUid() {
        return JomenderJadePlugin.GENERATOR_ENERGY;
    }
}
