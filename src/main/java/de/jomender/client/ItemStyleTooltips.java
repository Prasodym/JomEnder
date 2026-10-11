package de.jomender.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.jomender.misc.ItemStyles;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Zeigt die Shift-Infos aus {@link ItemStyles} im Tooltip an. */
public final class ItemStyleTooltips {
    private ItemStyleTooltips() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStyles.Style style = ItemStyles.get(event.getItemStack());
        if (style == null || style.info().isEmpty()) {
            return;
        }

        if (isShiftDown()) {
            for (ItemStyles.InfoLine line : style.info()) {
                event.getToolTip().add(line.text().copy().withColor(line.color()));
            }
        } else {
            event.getToolTip().add(Component.translatable(ChatFormatting.DARK_GRAY + "Hold" + ChatFormatting.WHITE + " [" + ChatFormatting.GRAY +  "SHIFT" +ChatFormatting.WHITE + "] " + ChatFormatting.DARK_GRAY + " for infomation")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }
}
