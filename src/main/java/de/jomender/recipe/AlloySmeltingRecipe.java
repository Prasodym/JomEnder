package de.jomender.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jomender.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Legierungsrezept des Electric Smelters (funktioniert wie der Alloy Smelter aus EnderIO).
 * Rezepte liegen in data/<namespace>/recipe/alloy_smelting/*.json:
 *
 * <pre>
 * {
 *   "type": "jomender:alloy_smelting",
 *   "slot_a": { "item": "minecraft:iron_ingot", "count": 2 },
 *   "slot_b": { "item": "minecraft:coal" },              (optional)
 *   "slot_c": { "item": "#c:dusts/redstone" },           (optional, Tags gehen auch)
 *   "result": { "id": "jomender:steel_ingot", "count": 1 },
 *   "energy": 4000,                                      (optional, FE gesamt, Standard 4000)
 *   "time": 200                                          (optional, Ticks, Standard 200 = 10 s)
 * }
 * </pre>
 * Welche Zutat in welchem Slot liegt, ist egal - Hauptsache, alle drei Slots zusammen passen.
 * Legierungsrezepte haben Vorrang vor normalen Ofenrezepten.
 */
public class AlloySmeltingRecipe implements Recipe<AlloySmeltingInput> {

    public static final int DEFAULT_ENERGY = 4000;
    public static final int DEFAULT_TIME = 200;

    /** Eine Zutat: Item (oder Tag) + Anzahl. */
    public record Part(Ingredient item, int count) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Part::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Part::count)
        ).apply(i, Part::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Part::item,
                ByteBufCodecs.VAR_INT, Part::count,
                Part::new);

        public boolean fits(ItemStack stack) {
            return !stack.isEmpty() && item.test(stack) && stack.getCount() >= count;
        }
    }

    public static final MapCodec<AlloySmeltingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Part.CODEC.fieldOf("slot_a").forGetter(AlloySmeltingRecipe::slotA),
            Part.CODEC.optionalFieldOf("slot_b").forGetter(AlloySmeltingRecipe::slotB),
            Part.CODEC.optionalFieldOf("slot_c").forGetter(AlloySmeltingRecipe::slotC),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloySmeltingRecipe::result),
            Codec.intRange(1, 10_000_000).optionalFieldOf("energy", DEFAULT_ENERGY).forGetter(AlloySmeltingRecipe::energy),
            Codec.intRange(1, 32_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(AlloySmeltingRecipe::time)
    ).apply(i, AlloySmeltingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmeltingRecipe> STREAM_CODEC = StreamCodec.composite(
            Part.STREAM_CODEC, AlloySmeltingRecipe::slotA,
            ByteBufCodecs.optional(Part.STREAM_CODEC), AlloySmeltingRecipe::slotB,
            ByteBufCodecs.optional(Part.STREAM_CODEC), AlloySmeltingRecipe::slotC,
            ItemStackTemplate.STREAM_CODEC, AlloySmeltingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloySmeltingRecipe::energy,
            ByteBufCodecs.VAR_INT, AlloySmeltingRecipe::time,
            AlloySmeltingRecipe::new);

    public static final RecipeSerializer<AlloySmeltingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Part slotA;
    private final Optional<Part> slotB;
    private final Optional<Part> slotC;
    private final ItemStackTemplate result;
    private final int energy;
    private final int time;
    private final List<Part> parts;

    public AlloySmeltingRecipe(Part slotA, Optional<Part> slotB, Optional<Part> slotC,
                               ItemStackTemplate result, int energy, int time) {
        this.slotA = slotA;
        this.slotB = slotB;
        this.slotC = slotC;
        this.result = result;
        this.energy = energy;
        this.time = time;
        List<Part> list = new ArrayList<>(3);
        list.add(slotA);
        slotB.ifPresent(list::add);
        slotC.ifPresent(list::add);
        this.parts = List.copyOf(list);
    }

    public Part slotA() { return slotA; }
    public Optional<Part> slotB() { return slotB; }
    public Optional<Part> slotC() { return slotC; }
    public ItemStackTemplate result() { return result; }
    public int energy() { return energy; }
    public int time() { return time; }

    /** Alle Zutaten (1 bis 3). */
    public List<Part> parts() { return parts; }

    /**
     * Welcher Eingangsslot liefert welche Zutat? Ergebnis[i] = Slot für parts().get(i),
     * oder null, wenn das Rezept nicht passt. Jeder Slot wird höchstens einmal benutzt.
     */
    public int @Nullable [] assign(AlloySmeltingInput input) {
        int[] slots = new int[parts.size()];
        return assign(input, 0, new boolean[3], slots) ? slots : null;
    }

    private boolean assign(AlloySmeltingInput input, int part, boolean[] used, int[] slots) {
        if (part == parts.size()) return true;
        for (int slot = 0; slot < 3; slot++) {
            if (!used[slot] && parts.get(part).fits(input.getItem(slot))) {
                used[slot] = true;
                slots[part] = slot;
                if (assign(input, part + 1, used, slots)) return true;
                used[slot] = false;
            }
        }
        return false;
    }

    @Override
    public boolean matches(AlloySmeltingInput input, Level level) {
        return assign(input) != null;
    }

    @Override
    public ItemStack assemble(AlloySmeltingInput input) {
        return result.create();
    }

    public ItemStack resultStack() {
        return result.create();
    }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeSerializer<AlloySmeltingRecipe> getSerializer() { return ModRecipes.ALLOY_SMELTING_SERIALIZER.get(); }

    @Override
    public RecipeType<AlloySmeltingRecipe> getType() { return ModRecipes.ALLOY_SMELTING_TYPE.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
