package de.jomender.compat.jade;

import de.jomender.blockentity.CoalGeneratorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class GeneratorEnergyTooltip implements IBlockComponentProvider {

    public static final GeneratorEnergyTooltip INSTANCE =
            new GeneratorEnergyTooltip();

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
    ) {
        GeneratorEnergyData.INSTANCE
                .decodeFromData(accessor)
                .ifPresent(energy -> {
                    int maxEnergy = CoalGeneratorBlockEntity.MAX_ENERGY;

                    int percent = (int) ((energy / (float) maxEnergy) * 100.0f);
                    String bar = buildBar(energy, maxEnergy, 20);

                    tooltip.add(Component.literal(
                            "Energy: " + energy + " / " + maxEnergy + " FE"
                    ));

                    tooltip.add(Component.literal(
                            bar + " " + percent + "%"
                    ));
                });
    }

    private String buildBar(int value, int max, int length) {
        if (max <= 0) {
            return "░".repeat(length);
        }

        int filled = Math.round((value / (float) max) * length);
        filled = Math.max(0, Math.min(length, filled));

        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    @Override
    public Identifier getUid() {
        return JomenderJadePlugin.GENERATOR_ENERGY;
    }
}
