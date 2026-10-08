
package de.jomender.misc;

import de.jomender.ModBlocks;
import de.jomender.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class GeneratorItemTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {

        ItemStack stack = event.getItemStack();

        // Nur beim Coal Generator anzeigen
        if (!stack.is(ModBlocks.COAL_GENERATOR.asItem())) {
            return;
        }

        // Gespeicherte Energie auslesen
        int energy = stack.getOrDefault(
                ModDataComponents.GENERATOR_ENERGY.get(),
                0
        );

        // Energie begrenzen
        energy = Math.clamp(energy, 0, 20000);

        String text = String.format(
                "Energie: %,d / 20.000 FE",
                energy
        );

        event.getToolTip().add(
                Component.literal(text)
                        .withStyle(ChatFormatting.RED)
        );
    }
}
