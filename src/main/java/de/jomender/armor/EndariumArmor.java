package de.jomender.armor;

import de.jomender.Jomender;
import de.jomender.item.EndariumTools;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Endarium-Rüstungsmaterial: eine Stufe über Netherite. */
public final class EndariumArmor {
    private EndariumArmor() {}

    /** Verweist auf assets/jomender/equipment/endarium.json (Texturen am Körper). */
    public static final ResourceKey<EquipmentAsset> ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "endarium"));

    /**
     * Zum Vergleich Netherite: Rüstung 3/6/8/3, Härte 3, Rückstoß-Resistenz 0.1, Verzauberbarkeit 15.
     * Haltbarkeit ist egal - die Teile sind unzerstörbar.
     */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            50,
            Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 20),
            20,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.15F,
            EndariumTools.ENDARIUM_TOOL_MATERIALS,
            ASSET
    );
}
