package de.jomender.client;

import de.jomender.ModRecipes;
import de.jomender.recipe.ArcFusionRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.List;

/** Merkt sich die vom Server geschickten Arc-Fusion-Rezepte (für JEI). */
public final class ClientRecipeCache {
    private ClientRecipeCache() {}

    private static List<RecipeHolder<ArcFusionRecipe>> arcFusion = List.of();

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        arcFusion = List.copyOf(event.getRecipeMap().byType(ModRecipes.ARC_FUSION_TYPE.get()));
    }

    public static List<RecipeHolder<ArcFusionRecipe>> arcFusionRecipes() {
        return arcFusion;
    }
}
