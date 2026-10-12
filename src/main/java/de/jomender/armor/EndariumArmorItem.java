package de.jomender.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Endarium-Rüstungsteil. Einziger Unterschied zu normaler Rüstung:
 * Beim Upgrade am Schmiedetisch werden mehrere Upgrade-Items auf einmal verbraucht
 * (Vanilla nimmt immer nur 1 - den Rest ziehen wir hier ab).
 */
public class EndariumArmorItem extends Item {
    public EndariumArmorItem(Item.Properties properties) {
        super(properties);
    }

    public static EquipmentSlot slotOf(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.MAINHAND;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        if (player.level().isClientSide() || !(player.containerMenu instanceof SmithingMenu menu)) {
            return;
        }
        // Wird aufgerufen, BEVOR der Schmiedetisch je 1 Item aus den Slots nimmt
        ItemStack base = menu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        if (!(base.getItem() instanceof EndariumArmorItem)) {
            return; // Netherite -> Endarium, kein Upgrade
        }
        ItemStack addition = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        ArmorUpgrade upgrade = ArmorUpgrade.find(slotOf(base), addition);
        if (upgrade != null && upgrade.cost() > 1) {
            addition.shrink(upgrade.cost() - 1);
        }
    }
}
