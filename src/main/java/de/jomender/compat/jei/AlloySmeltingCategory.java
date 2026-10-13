package de.jomender.compat.jei;

import de.jomender.ModBlocks;
import de.jomender.recipe.AlloySmeltingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI-Seite "Alloy Smelting" des Electric Smelters (1 bis 3 Zutaten). */
public class AlloySmeltingCategory extends AbstractRecipeCategory<AlloySmeltingRecipe> {
    public static final IRecipeType<AlloySmeltingRecipe> TYPE =
            IRecipeType.create("jomender", "alloy_smelting", AlloySmeltingRecipe.class);

    public AlloySmeltingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.jomender.alloy_smelting"),
                guiHelper.createDrawableItemLike(ModBlocks.ELECTRIC_SMELTER.get()), 116, 40);
    }

    @Override
    @SuppressWarnings("deprecation") // Ingredient.items(): für die JEI-Anzeige gedacht
    public void setRecipe(IRecipeLayoutBuilder builder, AlloySmeltingRecipe recipe, IFocusGroup focuses) {
        List<AlloySmeltingRecipe.Part> parts = recipe.parts();
        for (int i = 0; i < 3; i++) {
            var slot = builder.addInputSlot(1 + i * 18, 4).setStandardSlotBackground();
            if (i < parts.size()) {
                AlloySmeltingRecipe.Part part = parts.get(i);
                slot.addItemStacks(part.item().items().map(item -> new ItemStack(item, part.count())).toList());
            }
        }
        builder.addOutputSlot(93, 4).setOutputSlotBackground().add(recipe.resultStack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AlloySmeltingRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(recipe.time()).setPosition(62, 4);
    }

    @Override
    public void draw(AlloySmeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        String info = String.format("%,d FE  •  %.1fs", recipe.energy(), recipe.time() / 20.0);
        graphics.text(Minecraft.getInstance().font, Component.literal(info), 1, 28, 0xFF606060, false);
    }
}
