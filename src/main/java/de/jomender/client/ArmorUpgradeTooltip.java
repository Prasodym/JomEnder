package de.jomender.client;

import de.jomender.armor.ArmorUpgrade;
import de.jomender.armor.ArmorUpgrades;
import de.jomender.armor.EndariumArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Listet die eingebauten Upgrades im Tooltip der Endarium-Rüstung auf. */
public final class ArmorUpgradeTooltip {
    private ArmorUpgradeTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EndariumArmorItem)) return;
        ArmorUpgrades upgrades = ArmorUpgrades.of(stack);
        if (upgrades.isEmpty()) return;

        // Direkt unter den Namen setzen
        int index = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(index++, Component.translatable("tooltip.jomender.armor_upgrades").withStyle(ChatFormatting.GRAY));
        for (ArmorUpgrade upgrade : ArmorUpgrade.values()) {
            int level = upgrades.level(upgrade);
            if (level <= 0) continue;
            MutableComponent line = Component.literal(" ▸ ").append(upgrade.displayName());
            if (upgrade.stackable()) {
                line.append(" " + level + "/" + upgrade.maxLevel());
            }
            event.getToolTip().add(index++, line.withColor(0xB06CFF));
        }
    }
}
