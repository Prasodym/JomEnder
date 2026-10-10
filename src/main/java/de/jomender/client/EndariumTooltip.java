package de.jomender.client;

import de.jomender.ModBlocks;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class EndariumTooltip {

    public static void onTooltip(ItemTooltipEvent event) {

        if (!event.getItemStack().is(
                ModBlocks.ENDARIUM_BLOCK.get().asItem()
        )) {
            return;
        }

        boolean shift = InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow(),
                InputConstants.KEY_LSHIFT
        ) || InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow(),
                InputConstants.KEY_RSHIFT
        );

        if (shift) {
            event.getToolTip().add(
                    Component.translatable(
                            "block.jomender.endarium_block.tooltip"
                    ).withStyle(ChatFormatting.GRAY)
            );

            event.getToolTip().add(
                    Component.translatable(
                            "block.jomender.endarium_block.info"
                    ).withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        } else {
            event.getToolTip().add(
                    Component.literal("Halte [SHIFT] für weitere Informationen")
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }
}