package de.jomender.upgrade;

import de.jomender.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * Upgrade-Regeln für alle JomEnder-Maschinen.
 *
 * Speed:     jedes Upgrade verdoppelt das Tempo (1 = 2x, 2 = 4x, 3 = 8x ... 8 = 256x),
 *            kostet aber +25 % Energie pro Vorgang.
 * Efficient: jedes Upgrade senkt die Energie pro Vorgang um 20 % (8 = ca. 17 %).
 * Pro Sorte zählen maximal 8 Upgrades, und jede Sorte darf nur in einem Slot liegen.
 */
public final class Upgrades {
    private Upgrades() {}

    public static final int MAX_PER_TYPE = 8;
    public static final double SPEED_ENERGY_PENALTY = 0.25;
    public static final double EFFICIENCY_FACTOR = 0.8;

    public static boolean isUpgrade(ItemStack stack) {
        return stack.is(ModItems.Speed_upgrade.get()) || stack.is(ModItems.Efficient_upgrade.get());
    }

    /** Anzahl einer Upgrade-Sorte in den Upgrade-Slots, höchstens 8. */
    public static int count(Container container, int start, int count, Item item) {
        int total = 0;
        for (int i = start; i < start + count; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return Math.min(total, MAX_PER_TYPE);
    }

    public static int speedLevel(Container container, int start, int count) {
        return count(container, start, count, ModItems.Speed_upgrade.get());
    }

    public static int efficiencyLevel(Container container, int start, int count) {
        return count(container, start, count, ModItems.Efficient_upgrade.get());
    }

    /** Tempo-Faktor: 2 hoch Anzahl Speed-Upgrades. */
    public static int speedFactor(int speedLevel) {
        return 1 << Math.clamp(speedLevel, 0, MAX_PER_TYPE);
    }

    /** Faktor für die Energie pro Vorgang (Speed verteuert, Efficient verbilligt). */
    public static double energyMultiplier(int speedLevel, int efficiencyLevel) {
        return (1.0 + SPEED_ENERGY_PENALTY * speedLevel) * Math.pow(EFFICIENCY_FACTOR, efficiencyLevel);
    }

    /**
     * Berechnet Dauer und FE/t eines Vorgangs mit Upgrades.
     *
     * @param baseTime     Dauer ohne Upgrades in Ticks
     * @param baseEnergy   Energie pro Vorgang ohne Upgrades
     * @param capacity     Energiespeicher der Maschine - pro Tick wird höchstens die Hälfte verlangt,
     *                     damit die Maschine nie mehr braucht, als sie speichern kann
     */
    public static Process process(int baseTime, int baseEnergy, int speedLevel, int efficiencyLevel, int capacity) {
        long energy = Math.max(1, Math.round(baseEnergy * energyMultiplier(speedLevel, efficiencyLevel)));
        int time = Math.max(1, (int) Math.ceil(baseTime / (double) speedFactor(speedLevel)));
        // Sicherheitsnetz: FE/t darf die halbe Kapazität nicht überschreiten.
        int minTimeForCapacity = (int) Math.ceil(energy / (capacity / 2.0));
        time = Math.max(time, minTimeForCapacity);
        int perTick = (int) Math.max(1, (energy + time - 1) / time);
        return new Process(time, perTick);
    }

    /** Ergebnis von {@link #process}: Dauer in Ticks und FE pro Tick. */
    public record Process(int time, int energyPerTick) {}

    /** Darf dieses Item in diesen Upgrade-Slot? Nur Upgrades, jede Sorte nur in einem Slot. */
    public static boolean mayPlace(Container container, int start, int count, int slotIndex, ItemStack stack) {
        if (!isUpgrade(stack)) return false;
        for (int i = start; i < start + count; i++) {
            if (i != slotIndex && container.getItem(i).is(stack.getItem())) return false;
        }
        return true;
    }

    /** Fertiger Upgrade-Slot für die Menüs: nur Upgrades, max. 8 pro Slot, sichtbar laut active. */
    public static Slot slot(Container container, int index, int x, int y,
                            int start, int count, BooleanSupplier active) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Upgrades.mayPlace(container, start, count, index, stack);
            }

            @Override
            public int getMaxStackSize() {
                return MAX_PER_TYPE;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return MAX_PER_TYPE;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }
        };
    }
}
