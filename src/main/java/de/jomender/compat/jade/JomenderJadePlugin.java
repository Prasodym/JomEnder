
package de.jomender.compat.jade;

import de.jomender.Jomender;
import de.jomender.block.CoalGeneratorBlock;
import de.jomender.blockentity.CoalGeneratorBlockEntity;
import net.minecraft.resources.Identifier;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JomenderJadePlugin implements IWailaPlugin {

    public static final Identifier GENERATOR_ENERGY =
            Identifier.fromNamespaceAndPath(
                    Jomender.MOD_ID,
                    "generator_energy"
            );

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                GeneratorEnergyData.INSTANCE,
                CoalGeneratorBlockEntity.class
        );
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                GeneratorEnergyTooltip.INSTANCE,
                CoalGeneratorBlock.class
        );
    }
}
