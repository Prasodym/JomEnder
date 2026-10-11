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

/**
 * Rezept der Arc Fusion Chamber. Wird aus data/<namespace>/recipe/*.json geladen:
 *
 * <pre>
 * {
 *   "type": "jomender:arc_fusion",
 *   "input_a": "jomender:endarium_raw",
 *   "input_b": "jomender:galaxis_quarz",
 *   "count_a": 1,            (optional, Standard 1)
 *   "count_b": 1,            (optional, Standard 1)
 *   "result": { "id": "jomender:endarium_ingot", "count": 1 },
 *   "energy": 10000,         (optional, FE gesamt, Standard 10000)
 *   "time": 200              (optional, Ticks, Standard 200 = 10 Sekunden)
 * }
 * </pre>
 * Die Reihenfolge der Inputs ist egal: A darf in Slot 1 oder 2 liegen.
 * Inputs können auch Tags sein, z.B. "#c:ingots/iron".
 */
public class ArcFusionRecipe implements Recipe<ArcFusionInput> {

    public static final int DEFAULT_ENERGY = 10_000;
    public static final int DEFAULT_TIME = 200;

    public static final MapCodec<ArcFusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input_a").forGetter(ArcFusionRecipe::inputA),
            Ingredient.CODEC.fieldOf("input_b").forGetter(ArcFusionRecipe::inputB),
            Codec.intRange(1, 64).optionalFieldOf("count_a", 1).forGetter(ArcFusionRecipe::countA),
            Codec.intRange(1, 64).optionalFieldOf("count_b", 1).forGetter(ArcFusionRecipe::countB),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ArcFusionRecipe::result),
            Codec.intRange(1, 10_000_000).optionalFieldOf("energy", DEFAULT_ENERGY).forGetter(ArcFusionRecipe::energy),
            Codec.intRange(1, 32_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(ArcFusionRecipe::time)
    ).apply(i, ArcFusionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcFusionRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ArcFusionRecipe::inputA,
            Ingredient.CONTENTS_STREAM_CODEC, ArcFusionRecipe::inputB,
            ByteBufCodecs.VAR_INT, ArcFusionRecipe::countA,
            ByteBufCodecs.VAR_INT, ArcFusionRecipe::countB,
            ItemStackTemplate.STREAM_CODEC, ArcFusionRecipe::result,
            ByteBufCodecs.VAR_INT, ArcFusionRecipe::energy,
            ByteBufCodecs.VAR_INT, ArcFusionRecipe::time,
            ArcFusionRecipe::new
    );

    public static final RecipeSerializer<ArcFusionRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient inputA;
    private final Ingredient inputB;
    private final int countA;
    private final int countB;
    private final ItemStackTemplate result;
    private final int energy;
    private final int time;

    public ArcFusionRecipe(Ingredient inputA, Ingredient inputB, int countA, int countB,
                           ItemStackTemplate result, int energy, int time) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.countA = countA;
        this.countB = countB;
        this.result = result;
        this.energy = energy;
        this.time = time;
    }

    public Ingredient inputA() { return inputA; }
    public Ingredient inputB() { return inputB; }
    public int countA() { return countA; }
    public int countB() { return countB; }
    public ItemStackTemplate result() { return result; }
    public int energy() { return energy; }
    public int time() { return time; }

    /** FE pro Tick, aufgerundet, damit insgesamt mindestens "energy" verbraucht wird. */
    public int energyPerTick() {
        return Math.max(1, (energy + time - 1) / time);
    }

    private boolean fits(Ingredient ingredient, int count, ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
    }

    /** true, wenn A im ersten und B im zweiten Slot liegt (sonst umgekehrt). */
    public boolean isDirectOrder(ArcFusionInput input) {
        return fits(inputA, countA, input.first()) && fits(inputB, countB, input.second());
    }

    @Override
    public boolean matches(ArcFusionInput input, Level level) {
        return isDirectOrder(input)
                || (fits(inputA, countA, input.second()) && fits(inputB, countB, input.first()));
    }

    /** Wie viele Items aus Slot 1 bzw. 2 verbraucht werden. */
    public int countForFirstSlot(ArcFusionInput input) {
        return isDirectOrder(input) ? countA : countB;
    }

    public int countForSecondSlot(ArcFusionInput input) {
        return isDirectOrder(input) ? countB : countA;
    }

    @Override
    public ItemStack assemble(ArcFusionInput input) {
        return result.create();
    }

    /** Ergebnis ohne Input, z.B. um Platz in den Output-Slots zu prüfen. */
    public ItemStack resultStack() {
        return result.create();
    }

    // Kein Rezeptbuch für diese Maschine.
    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeSerializer<ArcFusionRecipe> getSerializer() { return ModRecipes.ARC_FUSION_SERIALIZER.get(); }

    @Override
    public RecipeType<ArcFusionRecipe> getType() { return ModRecipes.ARC_FUSION_TYPE.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
