package de.jomender.crusher;

import de.jomender.ModRecipes;
import de.jomender.recipe.CrushingRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Findet das passende Zerkleinerungs-Rezept für ein Item. Reihenfolge:
 * <ol>
 *   <li>eigene JSON-Rezepte ("type": "jomender:crushing")</li>
 *   <li>übernommene Rezepte anderer Mods ({@link ForeignCrushingImporter})</li>
 *   <li>Tag-Automatik: c:ores/X, c:raw_materials/X, c:ingots/X, c:gems/X -> c:dusts/X</li>
 * </ol>
 * Die Tag-Automatik sorgt dafür, dass jedes Metall jeder Mod funktioniert,
 * solange irgendeine Mod einen passenden Staub (c:dusts/X) mitbringt.
 */
public final class CrusherRecipes {
    private CrusherRecipes() {}

    /** Eine Regel der Tag-Automatik. bonus = Chance auf einen zusätzlichen Staub. */
    public record TagRule(String prefix, int count, float bonus) {}

    public static final List<TagRule> TAG_RULES = List.of(
            new TagRule("ores", 2, 0F),
            new TagRule("raw_materials", 1, 0.33F),
            new TagRule("ingots", 1, 0F),
            new TagRule("gems", 1, 0F));

    public static final String SOURCE_TAGS = "tags";

    // Zwischenspeicher (nur Server). Wird neu aufgebaut, sobald sich die Rezeptliste ändert (/reload).
    private static @Nullable RecipeMap cachedMap;
    private static List<CrushingRecipe> imported = List.of();
    private static final Map<Item, Optional<CrushingRecipe>> BY_ITEM = new HashMap<>();

    /** Passendes Rezept für den Stapel, oder null. Die Anzahl prüft der Aufrufer (inputCount). */
    public static synchronized @Nullable CrushingRecipe find(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return null;
        RecipeMap map = level.getServer().getRecipeManager().recipeMap();
        if (map != cachedMap) {
            cachedMap = map;
            BY_ITEM.clear();
            imported = ForeignCrushingImporter.importAll(map.values(), level.registryAccess());
        }
        return BY_ITEM.computeIfAbsent(stack.getItem(), item -> Optional.ofNullable(lookup(map, stack)))
                .orElse(null);
    }

    private static @Nullable CrushingRecipe lookup(RecipeMap map, ItemStack stack) {
        for (RecipeHolder<CrushingRecipe> holder : map.byType(ModRecipes.CRUSHING_TYPE.get())) {
            if (holder.value().accepts(stack)) return holder.value();
        }
        for (CrushingRecipe recipe : imported) {
            if (recipe.accepts(stack)) return recipe;
        }
        return fromTags(stack);
    }

    // =========================
    // TAG-AUTOMATIK
    // =========================

    private static @Nullable CrushingRecipe fromTags(ItemStack stack) {
        for (TagRule rule : TAG_RULES) {
            String prefix = rule.prefix() + "/";
            for (TagKey<Item> tag : stack.typeHolder().tags().toList()) {
                Identifier id = tag.location();
                if (!id.getNamespace().equals("c") || !id.getPath().startsWith(prefix)) continue;
                String material = id.getPath().substring(prefix.length());
                CrushingRecipe recipe = autoRecipe(rule, tag, material);
                if (recipe != null && !recipe.maxResults().getFirst().is(stack.getItem())) {
                    return recipe;
                }
            }
        }
        return null;
    }

    /** Rezept "Quelle-Tag -> c:dusts/material", wenn es den Staub gibt. */
    private static @Nullable CrushingRecipe autoRecipe(TagRule rule, TagKey<Item> source, String material) {
        if (material.isEmpty() || material.contains("/")) return null;
        TagKey<Item> dust = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/" + material));
        if (CrushingRecipe.preferredItem(dust).isEmpty()) return null;
        Optional<HolderSet.Named<Item>> sourceSet = BuiltInRegistries.ITEM.get(source);
        if (sourceSet.isEmpty() || sourceSet.get().size() == 0) return null;

        List<CrushingRecipe.Output> outputs = new ArrayList<>(2);
        outputs.add(CrushingRecipe.Output.ofTag(dust, rule.count(), 1F));
        if (rule.bonus() > 0F) {
            outputs.add(CrushingRecipe.Output.ofTag(dust, 1, rule.bonus()));
        }
        return new CrushingRecipe(Ingredient.of(sourceSet.get()), 1, outputs,
                CrushingRecipe.DEFAULT_ENERGY, CrushingRecipe.DEFAULT_TIME).withSource(SOURCE_TAGS);
    }

    /**
     * Für JEI, nach denselben Regeln wie die Maschine: eigene Rezepte vor übernommenen vor der
     * Tag-Automatik. Rezepte, die komplett von einem wichtigeren Rezept verdeckt werden, fallen weg.
     */
    public static List<CrushingRecipe> forDisplay(List<CrushingRecipe> own, List<CrushingRecipe> imported) {
        List<CrushingRecipe> shown = new ArrayList<>(own);
        addUncovered(shown, imported);
        addUncovered(shown, allTagRecipes());
        return shown;
    }

    @SuppressWarnings("deprecation") // Ingredient.items(): nur für die Anzeige
    private static void addUncovered(List<CrushingRecipe> shown, List<CrushingRecipe> candidates) {
        List<CrushingRecipe> before = List.copyOf(shown);
        for (CrushingRecipe candidate : candidates) {
            boolean covered = candidate.input().items()
                    .allMatch(item -> before.stream().anyMatch(r -> r.accepts(new ItemStack(item))));
            if (!covered) shown.add(candidate);
        }
    }

    /** Alle Rezepte der Tag-Automatik (für JEI). */
    public static List<CrushingRecipe> allTagRecipes() {
        List<CrushingRecipe> list = new ArrayList<>();
        List<String> materials = BuiltInRegistries.ITEM.getTags()
                .map(set -> set.key().location())
                .filter(id -> id.getNamespace().equals("c") && id.getPath().startsWith("dusts/"))
                .map(id -> id.getPath().substring("dusts/".length()))
                .filter(m -> !m.isEmpty() && !m.contains("/"))
                .sorted()
                .toList();
        for (String material : materials) {
            for (TagRule rule : TAG_RULES) {
                TagKey<Item> source = TagKey.create(Registries.ITEM,
                        Identifier.fromNamespaceAndPath("c", rule.prefix() + "/" + material));
                CrushingRecipe recipe = autoRecipe(rule, source, material);
                if (recipe != null) list.add(recipe);
            }
        }
        return list;
    }
}
