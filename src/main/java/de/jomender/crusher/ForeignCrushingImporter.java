package de.jomender.crusher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import de.jomender.Jomender;
import de.jomender.recipe.CrushingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Übernimmt Zerkleinerungs-Rezepte anderer Mods (EnderIO SAG Mill, Mekanism Crusher,
 * Create Crushing Wheels, Thermal Pulverizer, Immersive Engineering Crusher, ...).
 *
 * <p>Es gibt keine feste Abhängigkeit zu diesen Mods: jedes Rezept wird mit seinem eigenen
 * Codec in JSON zurückverwandelt und dann nach den üblichen Feldern durchsucht
 * ("input"/"ingredients", "outputs"/"results", "chance", "count", "energy").
 * Rezepte mit mehr als einer Zutat werden übersprungen.</p>
 */
public final class ForeignCrushingImporter {
    private ForeignCrushingImporter() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Rezept-Typen, deren Name eines dieser Wörter enthält, gelten als "Zerkleinern". */
    private static final String[] KEYWORDS = {"crush", "grind", "mill", "pulveriz", "macerat"};
    /** ... außer Sägewerke (z.B. "sawmill"). */
    private static final String[] EXCLUDED = {"saw"};

    private static final String[] INPUT_KEYS = {"input", "ingredient", "ingredients", "inputs", "input_item", "item_input"};
    private static final String[] OUTPUT_KEYS = {"outputs", "output", "results", "result", "main_output",
            "secondary_output", "secondary_outputs", "secondaries", "extra_output", "bonus_output"};

    public static boolean isCrushingType(@Nullable Identifier type) {
        if (type == null) return false;
        String ns = type.getNamespace();
        if (ns.equals(Jomender.MOD_ID) || ns.equals("minecraft")) return false;
        String path = type.getPath().toLowerCase(Locale.ROOT);
        for (String ex : EXCLUDED) {
            if (path.contains(ex)) return false;
        }
        for (String key : KEYWORDS) {
            if (path.contains(key)) return true;
        }
        return false;
    }

    public static List<CrushingRecipe> importAll(Collection<RecipeHolder<?>> holders, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<CrushingRecipe> result = new ArrayList<>();
        int skipped = 0;
        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (!isCrushingType(type)) continue;
            try {
                JsonElement json = encode(recipe, ops);
                CrushingRecipe converted = json != null && json.isJsonObject()
                        ? convert(json.getAsJsonObject(), ops) : null;
                if (converted != null) {
                    result.add(converted.withSource(type.toString()));
                } else {
                    skipped++;
                }
            } catch (RuntimeException e) {
                skipped++;
                LOGGER.debug("Crusher: Rezept {} ({}) nicht übernommen: {}", holder.id().identifier(), type, e.toString());
            }
        }
        if (!result.isEmpty() || skipped > 0) {
            LOGGER.info("Crusher: {} Zerkleinerungs-Rezepte anderer Mods übernommen ({} übersprungen)", result.size(), skipped);
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable JsonElement encode(Recipe<?> recipe, RegistryOps<JsonElement> ops) {
        MapCodec codec = recipe.getSerializer().codec();
        return (JsonElement) codec.codec().encodeStart(ops, recipe).result().orElse(null);
    }

    // =========================
    // UMWANDLUNG
    // =========================

    private static @Nullable CrushingRecipe convert(JsonObject json, RegistryOps<JsonElement> ops) {
        JsonElement inputElement = first(json, INPUT_KEYS);
        if (inputElement == null) return null;
        int[] count = {1};
        Ingredient input = parseIngredient(inputElement, ops, count);
        if (input == null) return null;

        List<CrushingRecipe.Output> outputs = new ArrayList<>();
        for (String key : OUTPUT_KEYS) {
            JsonElement el = json.get(key);
            if (el != null) collectOutputs(el, 1F, outputs);
        }
        if (outputs.isEmpty()) return null;
        if (outputs.size() > CrushingRecipe.MAX_OUTPUTS) {
            outputs = outputs.subList(0, CrushingRecipe.MAX_OUTPUTS);
        }

        int energy = CrushingRecipe.DEFAULT_ENERGY;
        JsonElement e = json.get("energy");
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            energy = Math.clamp(e.getAsInt(), 1, 10_000_000);
        }
        return new CrushingRecipe(input, Math.clamp(count[0], 1, 64), outputs, energy, CrushingRecipe.DEFAULT_TIME);
    }

    private static @Nullable JsonElement first(JsonObject json, String[] keys) {
        for (String key : keys) {
            if (json.has(key)) return json.get(key);
        }
        return null;
    }

    /** Liest eine Zutat. count[0] bekommt die Anzahl, falls angegeben. */
    private static @Nullable Ingredient parseIngredient(JsonElement el, RegistryOps<JsonElement> ops, int[] count) {
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() != 1) {
                // Liste von Alternativen (["a", "b"]) ist ok, mehrere getrennte Zutaten nicht
                Optional<Ingredient> whole = Ingredient.CODEC.parse(ops, el).result();
                return arr.size() > 1 && arr.get(0).isJsonPrimitive() ? whole.orElse(null) : null;
            }
            return parseIngredient(arr.get(0), ops, count);
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            int n = intOf(obj, -1, "count", "amount");
            if (n > 0) count[0] = n;
            if (obj.has("ingredient")) return parseIngredient(obj.get("ingredient"), ops, count);
            if (obj.has("tag") && obj.get("tag").isJsonPrimitive()) {
                return parseIngredient(new JsonPrimitive("#" + obj.get("tag").getAsString()), ops, count);
            }
            if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
                return parseIngredient(obj.get("item"), ops, count);
            }
            if (obj.has("id") && obj.get("id").isJsonPrimitive()) {
                return parseIngredient(obj.get("id"), ops, count);
            }
            JsonObject copy = obj.deepCopy();
            copy.remove("count");
            copy.remove("amount");
            return Ingredient.CODEC.parse(ops, copy).result().orElse(null);
        }
        return Ingredient.CODEC.parse(ops, el).result().orElse(null);
    }

    private static void collectOutputs(JsonElement el, float chance, List<CrushingRecipe.Output> out) {
        if (el.isJsonArray()) {
            for (JsonElement child : el.getAsJsonArray()) collectOutputs(child, chance, out);
            return;
        }
        if (el.isJsonPrimitive()) {
            CrushingRecipe.Output o = outputFor(el.getAsString(), 1, chance);
            if (o != null) out.add(o);
            return;
        }
        if (!el.isJsonObject()) return;
        JsonObject obj = el.getAsJsonObject();
        float c = floatOf(obj, chance, "chance", "probability");
        int count = intOf(obj, 1, "count", "amount");

        String id = stringOf(obj, "id");
        if (id == null) id = stringOf(obj, "item");
        if (id != null) {
            CrushingRecipe.Output o = outputFor(id, count, c);
            if (o != null) out.add(o);
            return;
        }
        String tag = stringOf(obj, "tag");
        if (tag != null) {
            CrushingRecipe.Output o = outputFor("#" + tag, count, c);
            if (o != null) out.add(o);
            return;
        }
        // Verschachtelt, z.B. EnderIO: { "item": { "id": ... }, "chance": 0.8 }
        for (String key : new String[]{"item", "stack", "output", "result"}) {
            if (obj.has(key) && obj.get(key).isJsonObject()) {
                JsonObject inner = obj.getAsJsonObject(key).deepCopy();
                if (!inner.has("count") && !inner.has("amount") && count != 1) inner.addProperty("count", count);
                collectOutputs(inner, c, out);
                return;
            }
        }
    }

    private static CrushingRecipe.@Nullable Output outputFor(String id, int count, float chance) {
        if (chance <= 0F) return null;
        float c = Math.min(1F, chance);
        int n = Math.clamp(count, 1, 64);
        if (id.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(id.substring(1));
            if (tagId == null) return null;
            return CrushingRecipe.Output.ofTag(TagKey.create(Registries.ITEM, tagId), n, c);
        }
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) return null;
        Optional<Holder.Reference<Item>> holder = BuiltInRegistries.ITEM.get(itemId);
        if (holder.isEmpty() || holder.get().value() == Items.AIR) return null;
        return CrushingRecipe.Output.of(holder.get(), n, c);
    }

    private static @Nullable String stringOf(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static int intOf(JsonObject obj, int fallback, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return e.getAsInt();
        }
        return fallback;
    }

    private static float floatOf(JsonObject obj, float fallback, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return e.getAsFloat();
        }
        return fallback;
    }
}
