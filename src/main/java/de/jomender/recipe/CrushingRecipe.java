package de.jomender.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jomender.Jomender;
import de.jomender.ModRecipes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rezept des Crushers (zermahlt Items zu Staub). Liegt in data/<namespace>/recipe/crushing/*.json:
 *
 * <pre>
 * {
 *   "type": "jomender:crushing",
 *   "input": "#c:ores/iron",                         (Item, Tag oder Liste)
 *   "count": 1,                                      (optional, wie viele Input-Items)
 *   "outputs": [
 *     { "id": "jomender:iron_dust", "count": 2 },
 *     { "tag": "c:dusts/gold", "chance": 0.1 }        (Tags gehen auch, chance 0..1)
 *   ],
 *   "energy": 2000,                                  (optional, FE gesamt)
 *   "time": 100                                      (optional, Ticks)
 * }
 * </pre>
 * Dieselbe Klasse wird auch für übernommene Rezepte anderer Mods und für die
 * Tag-Automatik (Erz/Rohmetall/Barren -> Staub) benutzt, siehe {@link de.jomender.crusher.CrusherRecipes}.
 */
public class CrushingRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_ENERGY = 2000;
    public static final int DEFAULT_TIME = 100;
    public static final int MAX_OUTPUTS = 4;

    /** Ein Ergebnis: festes Item ODER erstes Item eines Tags, mit Anzahl und Chance. */
    public record Output(Optional<Holder<Item>> item, Optional<TagKey<Item>> tag, int count, float chance) {

        private static final Codec<Output> RAW_CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("id").forGetter(Output::item),
                TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(Output::tag),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Output::count),
                Codec.floatRange(0F, 1F).optionalFieldOf("chance", 1F).forGetter(Output::chance)
        ).apply(i, Output::new));

        public static final Codec<Output> CODEC = RAW_CODEC.validate(o -> o.item().isPresent() == o.tag().isPresent()
                ? DataResult.error(() -> "Crushing-Ergebnis braucht genau eins von \"id\" oder \"tag\"")
                : DataResult.success(o));

        public static final StreamCodec<RegistryFriendlyByteBuf, Output> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM)), Output::item,
                ByteBufCodecs.optional(TagKey.streamCodec(Registries.ITEM)), Output::tag,
                ByteBufCodecs.VAR_INT, Output::count,
                ByteBufCodecs.FLOAT, Output::chance,
                Output::new);

        public static Output of(Holder<Item> item, int count, float chance) {
            return new Output(Optional.of(item), Optional.empty(), count, chance);
        }

        public static Output ofTag(TagKey<Item> tag, int count, float chance) {
            return new Output(Optional.empty(), Optional.of(tag), count, chance);
        }

        /** Das konkrete Item (bei Tags: bevorzugt aus Jom Ender, dann Vanilla, dann alphabetisch). */
        public ItemStack stack() {
            Optional<Holder<Item>> holder = item.isPresent() ? item : tag.flatMap(CrushingRecipe::preferredItem);
            return holder.map(h -> new ItemStack(h, count)).orElse(ItemStack.EMPTY);
        }

        public boolean guaranteed() {
            return chance >= 1F;
        }
    }

    public static final MapCodec<CrushingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CrushingRecipe::inputCount),
            Output.CODEC.listOf(1, MAX_OUTPUTS).fieldOf("outputs").forGetter(CrushingRecipe::outputs),
            Codec.intRange(1, 10_000_000).optionalFieldOf("energy", DEFAULT_ENERGY).forGetter(CrushingRecipe::energy),
            Codec.intRange(1, 32_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(CrushingRecipe::time)
    ).apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::input,
            ByteBufCodecs.VAR_INT, CrushingRecipe::inputCount,
            Output.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OUTPUTS)), CrushingRecipe::outputs,
            ByteBufCodecs.VAR_INT, CrushingRecipe::energy,
            ByteBufCodecs.VAR_INT, CrushingRecipe::time,
            CrushingRecipe::new);

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient input;
    private final int inputCount;
    private final List<Output> outputs;
    private final int energy;
    private final int time;
    /** Woher das Rezept stammt (für JEI): "" = Jom-Ender-JSON, sonst z.B. "enderio:sag_milling" oder "tags". */
    private String source = "";

    public CrushingRecipe(Ingredient input, int inputCount, List<Output> outputs, int energy, int time) {
        this.input = input;
        this.inputCount = inputCount;
        this.outputs = List.copyOf(outputs);
        this.energy = energy;
        this.time = time;
    }

    public Ingredient input() { return input; }
    public int inputCount() { return inputCount; }
    public List<Output> outputs() { return outputs; }
    public int energy() { return energy; }
    public int time() { return time; }

    public String source() { return source; }

    public CrushingRecipe withSource(String source) {
        this.source = source;
        return this;
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.test(stack);
    }

    /** Alle Ergebnisse mit voller Anzahl (zum Prüfen, ob die Ausgabe Platz hat). */
    public List<ItemStack> maxResults() {
        List<ItemStack> list = new ArrayList<>(outputs.size());
        for (Output o : outputs) {
            ItemStack s = o.stack();
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    /** Ergebnisse für einen Durchgang - Chancen werden ausgewürfelt. */
    public List<ItemStack> rollResults(RandomSource random) {
        List<ItemStack> list = new ArrayList<>(outputs.size());
        for (Output o : outputs) {
            if (o.guaranteed() || random.nextFloat() < o.chance()) {
                ItemStack s = o.stack();
                if (!s.isEmpty()) list.add(s);
            }
        }
        return list;
    }

    /** Bevorzugtes Item eines Tags: Jom Ender, dann Minecraft, dann alphabetisch nach ID. */
    public static Optional<Holder<Item>> preferredItem(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.get(tag).flatMap(set -> set.stream()
                .min(Comparator.comparingInt(CrushingRecipe::namespaceRank)
                        .thenComparing(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse(""))));
    }

    private static int namespaceRank(Holder<Item> holder) {
        String ns = holder.unwrapKey().map(k -> k.identifier().getNamespace()).orElse("");
        return ns.equals(Jomender.MOD_ID) ? 0 : ns.equals("minecraft") ? 1 : 2;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        return accepts(stack) && stack.getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().stack();
    }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeSerializer<CrushingRecipe> getSerializer() { return ModRecipes.CRUSHING_SERIALIZER.get(); }

    @Override
    public RecipeType<CrushingRecipe> getType() { return ModRecipes.CRUSHING_TYPE.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
