package de.jomender;

import de.jomender.recipe.ArcFusionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Jomender.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Jomender.MOD_ID);

    /** "type": "jomender:arc_fusion" in den Rezept-JSONs. */
    public static final Supplier<RecipeType<ArcFusionRecipe>> ARC_FUSION_TYPE =
            RECIPE_TYPES.register("arc_fusion",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "arc_fusion")));

    public static final Supplier<RecipeSerializer<ArcFusionRecipe>> ARC_FUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("arc_fusion", () -> ArcFusionRecipe.SERIALIZER);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
