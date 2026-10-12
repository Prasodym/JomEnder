package de.jomender.armor;

import de.jomender.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Alle Upgrades für die Endarium-Rüstung.
 * Am Schmiedetisch: Vorlage-Slot LEER lassen, Rüstungsteil + Upgrade-Item rein.
 *
 * cost     = wie viele Items auf einmal verbraucht werden
 * maxLevel = 1 -> einmaliges Upgrade, >1 -> stapelbar bis zu dieser Stufe
 *
 * Neue Upgrades: hier eine Zeile ergänzen, die Wirkung in ArmorUpgradeEffects einbauen
 * und einen Namen in der en_us.json ("armor_upgrade.jomender.<id>") anlegen.
 */
public enum ArmorUpgrade {
    // HELM
    NIGHT_VISION("night_vision", () -> Items.GOLDEN_CARROT, 1, 1, EquipmentSlot.HEAD),
    WATER_BREATHING("water_breathing", () -> Items.HEART_OF_THE_SEA, 1, 1, EquipmentSlot.HEAD),
    // BRUSTPLATTE (Health geht in jedes Teil)
    FLIGHT("flight", () -> Items.NETHER_STAR, 1, 1, EquipmentSlot.CHEST),
    HEALTH("health", () -> ModItems.Endarium_Ingot.get(), 10, 10,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
    // HOSE
    FIRE_IMMUNITY("fire_immunity", () -> Items.MAGMA_CREAM, 1, 1, EquipmentSlot.LEGS),
    SPEED("speed", () -> Items.SUGAR, 16, 3, EquipmentSlot.LEGS),

    // STIEFEL
    NO_FALL_DAMAGE("no_fall_damage", () -> Items.PHANTOM_MEMBRANE, 1, 1, EquipmentSlot.FEET),
    JUMP("jump", () -> Items.RABBIT_FOOT, 1, 3, EquipmentSlot.FEET);

    private final String id;
    private final Set<EquipmentSlot> slots;
    private final Supplier<Item> item;
    private final int cost;
    private final int maxLevel;

    ArmorUpgrade(String id, Supplier<Item> item, int cost, int maxLevel, EquipmentSlot first, EquipmentSlot... more) {
        this.id = id;
        this.slots = EnumSet.of(first, more);
        this.item = item;
        this.cost = cost;
        this.maxLevel = maxLevel;
    }

    public String id() { return id; }
    /** In welche Rüstungsteile das Upgrade passt. */
    public Set<EquipmentSlot> slots() { return slots; }
    public Item item() { return item.get(); }
    public int cost() { return cost; }
    public int maxLevel() { return maxLevel; }
    public boolean stackable() { return maxLevel > 1; }

    public Component displayName() {
        return Component.translatable("armor_upgrade.jomender." + id);
    }

    /** Welches Upgrade passt zu diesem Rüstungs-Slot und diesem Item? */
    public static @Nullable ArmorUpgrade find(EquipmentSlot slot, ItemStack addition) {
        if (addition.isEmpty()) return null;
        for (ArmorUpgrade upgrade : values()) {
            if (upgrade.slots.contains(slot) && addition.is(upgrade.item())) {
                return upgrade;
            }
        }
        return null;
    }

    public static @Nullable ArmorUpgrade byId(String id) {
        for (ArmorUpgrade upgrade : values()) {
            if (upgrade.id.equals(id)) return upgrade;
        }
        return null;
    }
}
