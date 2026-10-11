package de.jomender.compat.jei;

import de.jomender.ModBlocks;
import de.jomender.recipe.ArcFusionRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/** JEI-Ansicht: zwei Inputs -> Pfeil -> Ergebnis, darunter FE und Dauer. */
public class ArcFusionCategory extends AbstractRecipeCategory<ArcFusionRecipe> {

    public static final IRecipeType<ArcFusionRecipe> TYPE =
            IRecipeType.create("jomender", "arc_fusion", ArcFusionRecipe.class);

    private static final int WIDTH = 112;
    private static final int HEIGHT = 40;

    private final IGuiHelper guiHelper;

    public ArcFusionCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("block.jomender.arc_fusion_chamber"),
                guiHelper.createDrawableItemLike(ModBlocks.ARC_FUSION_CHAMBER.get()), WIDTH, HEIGHT);
        this.guiHelper = guiHelper;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcFusionRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 4).setStandardSlotBackground()
                .addItemStacks(stacks(recipe.inputA(), recipe.countA()));
        builder.addInputSlot(21, 4).setStandardSlotBackground()
                .addItemStacks(stacks(recipe.inputB(), recipe.countB()));
        builder.addOutputSlot(85, 4).setOutputSlotBackground()
                .add(recipe.resultStack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ArcFusionRecipe recipe, IFocusGroup focuses) {
        builder.addDrawable(guiHelper.createAnimatedRecipeArrow(recipe.time()), 49, 4);
    }

    @Override
    public void draw(ArcFusionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        String info = String.format("%,d FE  •  %.1fs", recipe.energy(), recipe.time() / 20.0);
        graphics.text(Minecraft.getInstance().font, Component.literal(info), 1, 29, 0xFF606060, false);
    }

    /** Ingredient mit Stückzahl als Item-Liste (JEI zeigt dann z.B. "2x"). */
    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(item -> new ItemStack(item, count)).toList();
    }
}
