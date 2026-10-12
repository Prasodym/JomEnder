package de.jomender.compat.jei;

import de.jomender.Jomender;
import de.jomender.ModBlocks;
import de.jomender.client.ClientRecipeCache;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Wird von JEI automatisch gefunden (@JeiPlugin). Ohne JEI wird diese Klasse nie geladen. */
@JeiPlugin
public class JomEnderJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ArcFusionCategory(gui), new SmelterExclusiveCategory(gui),
                new MobDropCategory(gui), new ArmorUpgradeCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ArcFusionCategory.TYPE,
                ClientRecipeCache.arcFusionRecipes().stream().map(RecipeHolder::value).toList());
        registration.addRecipes(SmelterExclusiveCategory.TYPE, SmelterExclusiveCategory.recipes());
        registration.addRecipes(MobDropCategory.TYPE, MobDropCategory.recipes());
        registration.addRecipes(ArmorUpgradeCategory.TYPE, ArmorUpgradeCategory.recipes());

        // Reaktor: Bauanleitung als JEI-Infoseite (Taste "U" auf einem Reaktorblock)
        registration.addItemStackInfo(java.util.List.of(
                        new net.minecraft.world.item.ItemStack(de.jomender.ModItems.REACTOR_CONTROLLER.get()),
                        new net.minecraft.world.item.ItemStack(de.jomender.ModItems.REACTOR_CASING.get()),
                        new net.minecraft.world.item.ItemStack(de.jomender.ModItems.REACTOR_PORT.get())),
                net.minecraft.network.chat.Component.translatable("jei.jomender.reactor_info.1"),
                net.minecraft.network.chat.Component.translatable("jei.jomender.reactor_info.2"),
                net.minecraft.network.chat.Component.translatable("jei.jomender.reactor_info.3"),
                net.minecraft.network.chat.Component.translatable("jei.jomender.reactor_info.4"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(ArmorUpgradeCategory.TYPE, net.minecraft.world.level.block.Blocks.SMITHING_TABLE);
        registration.addCraftingStation(ArcFusionCategory.TYPE, ModBlocks.ARC_FUSION_CHAMBER.get());
        registration.addCraftingStation(SmelterExclusiveCategory.TYPE, ModBlocks.ELECTRIC_SMELTER.get());
        // Der Electric Smelter kann alle normalen Ofenrezepte
        registration.addCraftingStation(RecipeTypes.SMELTING, ModBlocks.ELECTRIC_SMELTER.get());
    }
}
