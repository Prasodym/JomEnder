package de.jomender.armor;

import de.jomender.Jomender;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Die eigentliche Wirkung der Rüstungs-Upgrades. */
public final class ArmorUpgradeEffects {
    private ArmorUpgradeEffects() {}

    /**
     * Attribut-Upgrades hängen direkt am Item (stehen dann auch im Tooltip "When on Body: ...")
     * und wirken nur, solange das Teil getragen wird.
     */
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EndariumArmorItem)) return;
        ArmorUpgrades upgrades = ArmorUpgrades.of(stack);
        if (upgrades.isEmpty()) return;

        EquipmentSlot slot = EndariumArmorItem.slotOf(stack);
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
        // IDs pro Slot eindeutig, sonst überschreiben sich z.B. Health von Helm und Brust gegenseitig
        String suffix = "_" + slot.getName();
        int level;

        // Jedes Teil: +1 Herz pro Stufe
        if ((level = upgrades.level(ArmorUpgrade.HEALTH)) > 0) {
            event.addModifier(Attributes.MAX_HEALTH,
                    modifier("upgrade_health" + suffix, 2.0 * level, AttributeModifier.Operation.ADD_VALUE), group);
        }
        // Brustplatte: Fliegen wie im Creative
        if (upgrades.level(ArmorUpgrade.FLIGHT) > 0) {
            event.addModifier(NeoForgeMod.CREATIVE_FLIGHT,
                    modifier("upgrade_flight" + suffix, 1.0, AttributeModifier.Operation.ADD_VALUE), group);
        }
        // Hose: +15 % Tempo pro Stufe
        if ((level = upgrades.level(ArmorUpgrade.SPEED)) > 0) {
            event.addModifier(Attributes.MOVEMENT_SPEED,
                    modifier("upgrade_speed" + suffix, 0.15 * level, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), group);
        }
        // Stiefel: höher springen (+ etwas mehr sichere Fallhöhe, damit man sich nicht selbst verletzt)
        if ((level = upgrades.level(ArmorUpgrade.JUMP)) > 0) {
            event.addModifier(Attributes.JUMP_STRENGTH,
                    modifier("upgrade_jump" + suffix, 0.1 * level, AttributeModifier.Operation.ADD_VALUE), group);
            event.addModifier(Attributes.SAFE_FALL_DISTANCE,
                    modifier("upgrade_jump" + suffix, 1.5 * level, AttributeModifier.Operation.ADD_VALUE), group);
        }
        // Stiefel: gar kein Fallschaden
        if (upgrades.level(ArmorUpgrade.NO_FALL_DAMAGE) > 0) {
            event.addModifier(Attributes.FALL_DAMAGE_MULTIPLIER,
                    modifier("upgrade_no_fall" + suffix, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }

    }

    /** Effekt-Upgrades: jede Sekunde auffrischen (ohne Partikel). */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;

        ArmorUpgrades head = upgradesIn(player, EquipmentSlot.HEAD);
        if (head.level(ArmorUpgrade.NIGHT_VISION) > 0) {
            refresh(player, MobEffects.NIGHT_VISION, 260); // > 200 Ticks, sonst flackert Nachtsicht
        }
        if (head.level(ArmorUpgrade.WATER_BREATHING) > 0) {
            refresh(player, MobEffects.WATER_BREATHING, 60);
        }
        if (upgradesIn(player, EquipmentSlot.LEGS).level(ArmorUpgrade.FIRE_IMMUNITY) > 0) {
            refresh(player, MobEffects.FIRE_RESISTANCE, 60);
        }
    }

    private static ArmorUpgrades upgradesIn(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof EndariumArmorItem ? ArmorUpgrades.of(stack) : ArmorUpgrades.EMPTY;
    }

    private static void refresh(Player player, Holder<MobEffect> effect, int ticks) {
        player.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, true));
    }

    private static AttributeModifier modifier(String name, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(Identifier.fromNamespaceAndPath(Jomender.MOD_ID, name), amount, op);
    }
}
