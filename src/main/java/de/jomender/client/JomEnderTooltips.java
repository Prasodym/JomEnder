
package de.jomender.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.jomender.ModBlocks;

import de.jomender.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class JomEnderTooltips {

    private JomEnderTooltips() {

    }



    // Hier definieren wir die Informationen für einen Block.
    private record TooltipInfo(
            String translationKey,
            ChatFormatting nameColor
    ) {
    }

    // Hier werden alle Blöcke und Items zugeordnet.
    private static TooltipInfo getTooltipInfo(ItemStack stack) {

        // Endarium-Block
        if (stack.is(ModBlocks.ENDARIUM_BLOCK.asItem())) {
            return new TooltipInfo(
                    "tooltip.jomender.endarium_block",
                    ChatFormatting.AQUA
            );
        }

        // Endarium-Rohblock
        if (stack.is(ModBlocks.Endarium_Raw_Block.asItem())) {
            return new TooltipInfo(
                    "tooltip.jomender.endarium_raw_block",
                    ChatFormatting.LIGHT_PURPLE
            );
        }

        if (stack.is(ModItems.GermaniumClump.asItem())) {
            return new TooltipInfo(
                    "tooltip.jomender.germanium_clumb",
                    ChatFormatting.LIGHT_PURPLE
            );
        }
        if(stack.is(ModItems.Endarium_Shword_Shard.asItem())) {
            return new TooltipInfo(
              "tooltip.jomender.endarium_shword_shard",ChatFormatting.LIGHT_PURPLE
            );
        }

        // Später weitere Blöcke hier ergänzen.

        return null;
    }

    public static void onTooltip(ItemTooltipEvent event) {

        TooltipInfo info = getTooltipInfo(event.getItemStack());

        System.out.println(
                "JomEnderTooltip: "
                        + event.getItemStack().getItem()
                        + " | Zeilen: "
                        + event.getToolTip().size()
        );

        // Kein konfigurierter JomEnder-Block?
        if (info == null) {
            return;
        }

        // Itemname einfärben
        if (!event.getToolTip().isEmpty()) {
            event.getToolTip().set(
                    0,
                    event.getToolTip().get(0)
                            .copy()
                            .withStyle(info.nameColor())
            );
        }

        boolean shiftDown =
                InputConstants.isKeyDown(
                        Minecraft.getInstance().getWindow(),
                        InputConstants.KEY_LSHIFT
                )
                        ||
                        InputConstants.isKeyDown(
                                Minecraft.getInstance().getWindow(),
                                InputConstants.KEY_RSHIFT
                        );

        if (shiftDown) {

            event.getToolTip().add(
                    Component.translatable(
                            info.translationKey() + ".description"
                    ).withStyle(ChatFormatting.GRAY)
            );

            event.getToolTip().add(
                    Component.translatable(
                            info.translationKey() + ".details"
                    ).withStyle(ChatFormatting.LIGHT_PURPLE)
            );

        } else {

            event.getToolTip().add(
                    Component.translatable(
                            "tooltip.jomender.hold_shift"
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }
}
