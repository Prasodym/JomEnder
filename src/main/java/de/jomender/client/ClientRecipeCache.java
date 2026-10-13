package de.jomender.client;

import de.jomender.ModRecipes;
import de.jomender.recipe.AlloySmeltingRecipe;
import de.jomender.recipe.ArcFusionRecipe;
import de.jomender.recipe.FluidInfusionRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.List;

/** Merkt sich die vom Server geschickten Arc-Fusion-Rezepte (für JEI). */
public final class ClientRecipeCache {
    private ClientRecipeCache() {}

    private static List<RecipeHolder<ArcFusionRecipe>> arcFusion = List.of();
    private static List<RecipeHolder<FluidInfusionRecipe>> fluidInfusion = List.of();
    private static List<RecipeHolder<AlloySmeltingRecipe>> alloySmelting = List.of();

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        arcFusion = List.copyOf(event.getRecipeMap().byType(ModRecipes.ARC_FUSION_TYPE.get()));
        fluidInfusion = List.copyOf(event.getRecipeMap().byType(ModRecipes.FLUID_INFUSION_TYPE.get()));
        alloySmelting = List.copyOf(event.getRecipeMap().byType(ModRecipes.ALLOY_SMELTING_TYPE.get()));
    }

    public static List<RecipeHolder<ArcFusionRecipe>> arcFusionRecipes() {
        return arcFusion;
    }

    public static List<RecipeHolder<FluidInfusionRecipe>> fluidInfusionRecipes() {
        return fluidInfusion;
    }

    public static List<RecipeHolder<AlloySmeltingRecipe>> alloySmeltingRecipes() {
        return alloySmelting;
    }
}
