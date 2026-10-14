package de.jomender.compat;

import de.jomender.ModRecipes;
import de.jomender.crusher.ForeignCrushingImporter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Seit Minecraft 1.21.2 bekommt der Client keine Rezepte mehr automatisch.
 * Damit JEI die Rezepte anzeigen kann, schickt der Server sie mit.
 */
public final class RecipeSync {
    private RecipeSync() {}

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.ARC_FUSION_TYPE.get(), ModRecipes.FLUID_INFUSION_TYPE.get(),
                ModRecipes.ALLOY_SMELTING_TYPE.get(), ModRecipes.CRUSHING_TYPE.get());

        // Zerkleinerungs-Rezepte anderer Mods ebenfalls schicken, damit JEI sie beim Crusher zeigt
        for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            if (ForeignCrushingImporter.isCrushingType(BuiltInRegistries.RECIPE_TYPE.getKey(type))) {
                event.sendRecipes(type);
            }
        }
    }
}
