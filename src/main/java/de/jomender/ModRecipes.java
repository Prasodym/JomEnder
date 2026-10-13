package de.jomender;

import de.jomender.recipe.ArcFusionRecipe;
import de.jomender.recipe.ArmorUpgradeRecipe;
import de.jomender.recipe.FluidInfusionRecipe;
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

    /** "type": "jomender:armor_upgrade" - Rüstungs-Upgrades am Schmiedetisch (Typ ist der normale Smithing-Typ). */
    public static final Supplier<RecipeSerializer<ArmorUpgradeRecipe>> ARMOR_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("armor_upgrade", () -> ArmorUpgradeRecipe.SERIALIZER);

    /** "type": "jomender:fluid_infusion" - Items in eine Flüssigkeit werfen. */
    public static final Supplier<RecipeType<FluidInfusionRecipe>> FLUID_INFUSION_TYPE =
            RECIPE_TYPES.register("fluid_infusion",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "fluid_infusion")));

    public static final Supplier<RecipeSerializer<FluidInfusionRecipe>> FLUID_INFUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fluid_infusion", () -> FluidInfusionRecipe.SERIALIZER);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
