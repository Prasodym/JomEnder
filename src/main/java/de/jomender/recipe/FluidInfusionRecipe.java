package de.jomender.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jomender.ModRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * Flüssigkeits-Infusion: Items in eine Flüssigkeit werfen -> Animation -> neues Item.
 * Rezepte liegen in data/<namespace>/recipe/fluid_infusion/*.json:
 *
 * <pre>
 * {
 *   "type": "jomender:fluid_infusion",
 *   "ingredients": [                                  (1 bis 3 verschiedene Items)
 *     { "item": "jomender:galaxis_shard", "count": 2 },
 *     { "item": "minecraft:quartz" }                 ("count" optional, Standard 1)
 *   ],
 *   "fluid": "minecraft:water",                      (z.B. auch "minecraft:lava")
 *   "result": { "id": "jomender:galaxis_quarz", "count": 1 },
 *   "time": 60,                                      (optional, Ticks Animation, Standard 60 = 3 s)
 *   "consume_fluid": false                           (optional, true = Quellblock verschwindet)
 * }
 * </pre>
 * "item" darf auch ein Tag sein, z.B. "#c:gems/quartz".
 */
public class FluidInfusionRecipe implements Recipe<FluidInfusionInput> {

    public static final int DEFAULT_TIME = 60;

    /** Ein Input: Item (oder Tag) + Anzahl. */
    public record Entry(Ingredient item, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new);

        /** Wie viele passende Items liegen insgesamt bereit? */
        int available(List<ItemStack> stacks) {
            int total = 0;
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty() && item.test(stack)) total += stack.getCount();
            }
            return total;
        }
    }

    public static final MapCodec<FluidInfusionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Entry.CODEC.listOf(1, 3).fieldOf("ingredients").forGetter(FluidInfusionRecipe::ingredients),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidInfusionRecipe::fluid),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(FluidInfusionRecipe::result),
            Codec.intRange(1, 1200).optionalFieldOf("time", DEFAULT_TIME).forGetter(FluidInfusionRecipe::time),
            Codec.BOOL.optionalFieldOf("consume_fluid", false).forGetter(FluidInfusionRecipe::consumeFluid)
    ).apply(i, FluidInfusionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidInfusionRecipe> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(3)), FluidInfusionRecipe::ingredients,
            ByteBufCodecs.registry(Registries.FLUID), FluidInfusionRecipe::fluid,
            ItemStackTemplate.STREAM_CODEC, FluidInfusionRecipe::result,
            ByteBufCodecs.VAR_INT, FluidInfusionRecipe::time,
            ByteBufCodecs.BOOL, FluidInfusionRecipe::consumeFluid,
            FluidInfusionRecipe::new);

    public static final RecipeSerializer<FluidInfusionRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<Entry> ingredients;
    private final Fluid fluid;
    private final ItemStackTemplate result;
    private final int time;
    private final boolean consumeFluid;

    public FluidInfusionRecipe(List<Entry> ingredients, Fluid fluid, ItemStackTemplate result, int time, boolean consumeFluid) {
        this.ingredients = List.copyOf(ingredients);
        this.fluid = fluid;
        this.result = result;
        this.time = time;
        this.consumeFluid = consumeFluid;
    }

    public List<Entry> ingredients() { return ingredients; }
    public Fluid fluid() { return fluid; }
    public ItemStackTemplate result() { return result; }
    public int time() { return time; }
    public boolean consumeFluid() { return consumeFluid; }

    @Override
    public boolean matches(FluidInfusionInput input, Level level) {
        // fließend oder Quelle zählt beides (isSame vergleicht Quelle/fließend)
        if (!fluid.isSame(input.fluid())) return false;
        for (Entry entry : ingredients) {
            if (entry.available(input.items()) < entry.count()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(FluidInfusionInput input) {
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
    public RecipeSerializer<FluidInfusionRecipe> getSerializer() { return ModRecipes.FLUID_INFUSION_SERIALIZER.get(); }

    @Override
    public RecipeType<FluidInfusionRecipe> getType() { return ModRecipes.FLUID_INFUSION_TYPE.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
