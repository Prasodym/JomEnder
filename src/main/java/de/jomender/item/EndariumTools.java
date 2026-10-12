package de.jomender.item;

import de.jomender.Jomender;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Endarium-Werkzeugstufe: über Netherite. */
public final class EndariumTools {
    private EndariumTools() {}

    /** Blöcke in diesem Tag kann nur Endarium-Werkzeug abbauen (Netherite und darunter nicht). */
    public static final TagKey<Block> NEEDS_ENDARIUM_TOOL = blockTag("needs_endarium_tool");
    /** Was Endarium-Werkzeug NICHT abbauen kann - leer, also alles. */
    public static final TagKey<Block> INCORRECT_FOR_ENDARIUM_TOOL = blockTag("incorrect_for_endarium_tool");
    /** Reparatur-Material (unwichtig, da unzerstörbar - aber vom Werkzeugsystem verlangt). */
    public static final TagKey<Item> ENDARIUM_TOOL_MATERIALS =
            TagKey.create(Registries.ITEM, id("endarium_tool_materials"));

    /**
     * Zum Vergleich Netherite: Haltbarkeit 2031, Tempo 9.0, Schaden +4, Verzauberbarkeit 15.
     * Die Haltbarkeit ist egal - die Spitzhacke bekommt zusätzlich die Komponente "unbreakable".
     */
    public static final ToolMaterial ENDARIUM = new ToolMaterial(
            INCORRECT_FOR_ENDARIUM_TOOL, 4062, 13.0F, 5.0F, 20, ENDARIUM_TOOL_MATERIALS);

    /** Schmiedevorlage: Netherite-Werkzeug + Endarium-Block -> Endarium-Werkzeug; Netherite-Schwert + Schwert-Splitter -> Endarium-Schwert. */
    public static SmithingTemplateItem createUpgradeTemplate(Item.Properties properties) {
        return new SmithingTemplateItem(
                text("applies_to"),
                text("ingredients"),
                text("base_slot_description"),
                text("additions_slot_description"),
                List.of(
                        Identifier.withDefaultNamespace("container/slot/pickaxe"),
                        Identifier.withDefaultNamespace("container/slot/axe"),
                        Identifier.withDefaultNamespace("container/slot/shovel"),
                        Identifier.withDefaultNamespace("container/slot/hoe"),
                        Identifier.withDefaultNamespace("container/slot/sword"),
                        Identifier.withDefaultNamespace("container/slot/helmet"),
                        Identifier.withDefaultNamespace("container/slot/chestplate"),
                        Identifier.withDefaultNamespace("container/slot/leggings"),
                        Identifier.withDefaultNamespace("container/slot/boots")),
                List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
                properties
        );
    }

    private static Component text(String key) {
        return Component.translatable("item.jomender.smithing_template.endarium_upgrade." + key);
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Jomender.MOD_ID, path);
    }
}
