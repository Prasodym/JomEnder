package de.jomender.compat;

import de.jomender.ModRecipes;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Seit Minecraft 1.21.2 bekommt der Client keine Rezepte mehr automatisch.
 * Damit JEI die Arc-Fusion-Rezepte anzeigen kann, schickt der Server sie mit.
 */
public final class RecipeSync {
    private RecipeSync() {}

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.ARC_FUSION_TYPE.get());
    }
}
