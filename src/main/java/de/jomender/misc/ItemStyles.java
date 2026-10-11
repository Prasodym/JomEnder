package de.jomender.misc;

import de.jomender.ModBlocks;
import de.jomender.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zentrale Stelle für Namen, Farben und Shift-Infos von JomEnder-Items.
 *
 * Neues Item eintragen: in {@link #define()} eine Zeile ergänzen, z.B.
 * <pre>
 * style(ModItems.Endarium_Ingot)
 *         .name("Endarium-Barren")              // optional, sonst Name aus der Sprachdatei
 *         .color(0xB06CFF)                      // Hex-Farbe oder ChatFormatting.AQUA
 *         .info("Erste Zeile bei SHIFT")         // grau
 *         .info("Zweite Zeile", ChatFormatting.LIGHT_PURPLE);
 * </pre>
 */
public final class ItemStyles {
    private ItemStyles() {}

    // =====================================================================
    // HIER ITEMS EINTRAGEN
    // =====================================================================

    private static void define() {

        style(ModBlocks.ENDARIUM_BLOCK)
                .color(ChatFormatting.AQUA)
                .info("A mysterious metal from the depths of the End.")
                .info("Charged Endarium energy flows through this block.", ChatFormatting.LIGHT_PURPLE);

        style(ModBlocks.Endarium_Raw_Block)
                .color(ChatFormatting.LIGHT_PURPLE)
                .info("Raw Endarium, still brimming with untamed energy.");

        style(ModItems.Endarium_Ingot)
                .color(0xB06CFF)
                .info("Fused in the Arc Fusion Chamber..", ChatFormatting.DARK_PURPLE);

        style(ModItems.Galaxis_Quarz)
                .color(0x5BE0F0)
                .info("There are traces of amethysts to be seen.");

        style(ModItems.GermaniumClump)
                .color(ChatFormatting.GREEN);

        style(ModItems.Speed_upgrade)
                .color(ChatFormatting.GREEN)
                .info("Increases the speed of the machine.", ChatFormatting.GRAY)
                .info("Each upgrade doubles the speed (2x, 4x, 8x ...)", ChatFormatting.AQUA)
                .info("+25% energy per operation each", ChatFormatting.RED)
                .info("Max. 8 per machine", ChatFormatting.DARK_GRAY);

        style(ModItems.Efficient_upgrade)
                .color(ChatFormatting.GREEN)
                .info("Increases the efficiency of the machine.", ChatFormatting.GRAY)
                .info("-20% energy per operation each", ChatFormatting.AQUA)
                .info("Generator: +25% energy per coal each", ChatFormatting.AQUA)
                .info("Max. 8 per machine", ChatFormatting.DARK_GRAY);

        style(ModItems.ENDARIUM_PICKAXE)
                .color(0xB06CFF)
                .info("Forged from Netherite and pure Endarium.")
                .info("Mines faster than Netherite.", ChatFormatting.AQUA)
                .info("Can mine blocks that need Endarium tools.", ChatFormatting.LIGHT_PURPLE);

        style(ModItems.ENDARIUM_UPGRADE_SMITHING_TEMPLATE)
                .color(0xB06CFF)
                .info("Found only in End City treasure chests.", ChatFormatting.LIGHT_PURPLE);

        // Uran: radioaktives Grün
        style(ModItems.Raw_Uranium)
                .color(0x7CFC00)
                .info("Found deep within the deepslate layer.");

        style(ModItems.Uranium_Dust)
                .color(0x7CFC00)
                .info("Can be smelted into an Uranium Ingot.");

        style(ModItems.Uranium_Ingot)
                .color(0x39FF14)
                .info("Highly energetic. Handle with care.")
                .info("Radioactive", ChatFormatting.GREEN);

        style(ModItems.URANIUM_BLOCK)
                .color(0x39FF14)
                .info("Nine Uranium Ingots, compressed.");

        // Weitere Items hier ergänzen ...
    }

    // =====================================================================
    // AB HIER: TECHNIK (muss normalerweise nicht angepasst werden)
    // =====================================================================

    /** Eine Tooltip-Zeile mit Farbe (ARGB). */
    public record InfoLine(Component text, int color) {}

    /** Alle Einstellungen für ein Item. */
    public static final class Style {
        private final ItemLike item;
        private String customName;
        private Integer color;
        private final List<InfoLine> info = new ArrayList<>();

        private Style(ItemLike item) {
            this.item = item;
        }

        /** Eigener Anzeigename. Ohne Aufruf wird der Name aus der Sprachdatei benutzt. */
        public Style name(String name) {
            this.customName = name;
            return this;
        }

        /** Namensfarbe als Hex, z.B. 0xB06CFF. */
        public Style color(int rgb) {
            this.color = rgb & 0xFFFFFF;
            return this;
        }

        /** Namensfarbe als Minecraft-Farbe, z.B. ChatFormatting.AQUA. */
        public Style color(ChatFormatting formatting) {
            Integer rgb = formatting.getColor();
            if (rgb != null) this.color = rgb;
            return this;
        }


        /** Zeile, die bei gedrückter SHIFT-Taste erscheint (grau). */
        public Style info(String text) {
            return info(text, ChatFormatting.GRAY);
        }

        public Style info(String text, ChatFormatting formatting) {
            Integer rgb = formatting.getColor();
            return info(text, rgb == null ? 0xAAAAAA : rgb);
        }

        public Style info(String text, int rgb) {
            info.add(new InfoLine(Component.literal(text), rgb & 0xFFFFFF));
            return this;
        }

        /** Wie info(), aber mit Übersetzungsschlüssel aus der Sprachdatei. */
        public Style infoKey(String translationKey, ChatFormatting formatting) {
            Integer rgb = formatting.getColor();
            info.add(new InfoLine(Component.translatable(translationKey), rgb == null ? 0xAAAAAA : rgb));
            return this;
        }

        public List<InfoLine> info() {
            return info;
        }

        /** Der Name, wie er im Spiel erscheinen soll (oder null, wenn nichts geändert wird). */
        private MutableComponent buildName(Item resolved) {
            if (customName == null && color == null) return null;
            MutableComponent name = customName != null
                    ? Component.literal(customName)
                    : Component.translatable(translationKey(resolved));
            return color != null ? name.withColor(color) : name;
        }
    }

    /** Gleicher Schlüssel wie in der Sprachdatei: block.jomender.xyz bzw. item.jomender.xyz */
    private static String translationKey(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String prefix = item instanceof BlockItem ? "block" : "item";
        return prefix + "." + id.getNamespace() + "." + id.getPath();
    }

    private static final Map<ItemLike, Style> STYLES = new LinkedHashMap<>();
    private static Map<Item, Style> byItem;

    private static Style style(ItemLike item) {
        return STYLES.computeIfAbsent(item, Style::new);
    }

    private static synchronized Map<Item, Style> resolved() {
        if (byItem == null) {
            if (STYLES.isEmpty()) define();
            Map<Item, Style> map = new LinkedHashMap<>();
            for (Style s : STYLES.values()) {
                map.put(s.item.asItem(), s);
            }
            byItem = map;
        }
        return byItem;
    }

    /** Style für einen Stack, oder null. */
    public static Style get(ItemStack stack) {
        return resolved().get(stack.getItem());
    }

    /**
     * Mod-Bus-Event: setzt Name und Farbe als Standard-Komponente ITEM_NAME.
     * Dadurch stimmt der Name überall: Tooltip, Hotbar, REI/JEI.
     */
    public static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        for (Map.Entry<Item, Style> entry : resolved().entrySet()) {
            MutableComponent name = entry.getValue().buildName(entry.getKey());
            if (name != null) {
                // Initializer-Variante (components, registries, item): seit NeoForge 26.1.2 empfohlen.
                event.modify(entry.getKey(),
                        (components, registries, item) -> components.set(DataComponents.ITEM_NAME, name));
            }
        }
    }
}
