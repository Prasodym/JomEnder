package de.jomender.client;

import de.jomender.ModRecipes;
import de.jomender.crusher.ForeignCrushingImporter;
import de.jomender.recipe.AlloySmeltingRecipe;
import de.jomender.recipe.ArcFusionRecipe;
import de.jomender.recipe.CrushingRecipe;
import de.jomender.recipe.FluidInfusionRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.ArrayList;
import java.util.List;

/** Merkt sich die vom Server geschickten Rezepte (für JEI). */
public final class ClientRecipeCache {
    private ClientRecipeCache() {}

    private static List<RecipeHolder<ArcFusionRecipe>> arcFusion = List.of();
    private static List<RecipeHolder<FluidInfusionRecipe>> fluidInfusion = List.of();
    private static List<RecipeHolder<AlloySmeltingRecipe>> alloySmelting = List.of();
    private static List<CrushingRecipe> crushingOwn = List.of();
    private static List<CrushingRecipe> crushingImported = List.of();

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        arcFusion = List.copyOf(event.getRecipeMap().byType(ModRecipes.ARC_FUSION_TYPE.get()));
        fluidInfusion = List.copyOf(event.getRecipeMap().byType(ModRecipes.FLUID_INFUSION_TYPE.get()));
        alloySmelting = List.copyOf(event.getRecipeMap().byType(ModRecipes.ALLOY_SMELTING_TYPE.get()));

        // Crusher: eigene Rezepte + übernommene Rezepte anderer Mods
        List<CrushingRecipe> own = new ArrayList<>();
        event.getRecipeMap().byType(ModRecipes.CRUSHING_TYPE.get()).forEach(h -> own.add(h.value()));
        crushingOwn = List.copyOf(own);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        crushingImported = connection == null ? List.of()
                : List.copyOf(ForeignCrushingImporter.importAll(event.getRecipeMap().values(), connection.registryAccess()));
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

    /** Eigene Crusher-Rezepte (jomender:crushing). */
    public static List<CrushingRecipe> ownCrushingRecipes() {
        return crushingOwn;
    }

    /** Übernommene Zerkleinerungs-Rezepte anderer Mods. */
    public static List<CrushingRecipe> importedCrushingRecipes() {
        return crushingImported;
    }
}
