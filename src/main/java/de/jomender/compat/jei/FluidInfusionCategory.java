package de.jomender.compat.jei;

import de.jomender.ModItems;
import de.jomender.recipe.FluidInfusionRecipe;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** JEI-Seite "Fluid Infusion": Items + Flüssigkeit -> Ergebnis. */
public class FluidInfusionCategory extends AbstractRecipeCategory<FluidInfusionRecipe> {
    public static final IRecipeType<FluidInfusionRecipe> TYPE =
            IRecipeType.create("jomender", "fluid_infusion", FluidInfusionRecipe.class);

    public FluidInfusionCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.jomender.fluid_infusion"),
                guiHelper.createDrawableItemLike(ModItems.Galaxis_Quarz.get()), 136, 42);
    }

    @Override
    @SuppressWarnings("deprecation") // Ingredient.items(): für die JEI-Anzeige gedacht
    public void setRecipe(IRecipeLayoutBuilder builder, FluidInfusionRecipe recipe, IFocusGroup focuses) {
        List<FluidInfusionRecipe.Entry> entries = recipe.ingredients();
        for (int i = 0; i < 3; i++) {
            var slot = builder.addInputSlot(1 + i * 18, 4).setStandardSlotBackground();
            if (i < entries.size()) {
                FluidInfusionRecipe.Entry entry = entries.get(i);
                slot.addItemStacks(entry.item().items().map(item -> new ItemStack(item, entry.count())).toList());
            }
        }
        // Flüssigkeit (Suche nach "Wasser" in JEI findet so auch diese Rezepte)
        builder.addInputSlot(60, 4).setStandardSlotBackground().add(recipe.fluid(), 1000);
        builder.addOutputSlot(113, 4).setOutputSlotBackground().add(recipe.resultStack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, FluidInfusionRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(recipe.time()).setPosition(83, 4);
    }

    @Override
    public void draw(FluidInfusionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Component fluidName = new FluidStack(recipe.fluid(), 1000).getHoverName();
        graphics.text(font, Component.translatable("jei.jomender.fluid_infusion.drop", fluidName), 1, 25, 0xFF606060, false);
        if (recipe.consumeFluid()) {
            graphics.text(font, Component.translatable("jei.jomender.fluid_infusion.consumes"), 1, 34, 0xFFB04040, false);
        }
    }
}
