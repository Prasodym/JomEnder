package de.jomender.compat.jei;

import de.jomender.ModBlocks;
import de.jomender.crusher.CrusherRecipes;
import de.jomender.recipe.CrushingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI-Seite "Crushing" des Crushers: 1 Zutat -> bis zu 4 Ergebnisse (mit Chance). */
public class CrushingCategory extends AbstractRecipeCategory<CrushingRecipe> {
    public static final IRecipeType<CrushingRecipe> TYPE =
            IRecipeType.create("jomender", "crushing", CrushingRecipe.class);

    private static final int OUTPUT_X = 58;
    private static final int OUTPUT_GAP = 22;
    private static final int SLOT_Y = 4;

    public CrushingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.jomender.crushing"),
                guiHelper.createDrawableItemLike(ModBlocks.CRUSHER.get()), 146, 46);
    }

    @Override
    @SuppressWarnings("deprecation") // Ingredient.items(): für die JEI-Anzeige gedacht
    public void setRecipe(IRecipeLayoutBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        int count = recipe.inputCount();
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground()
                .addItemStacks(recipe.input().items().map(item -> new ItemStack(item, count)).toList());

        List<CrushingRecipe.Output> outputs = recipe.outputs();
        for (int i = 0; i < outputs.size(); i++) {
            CrushingRecipe.Output output = outputs.get(i);
            ItemStack stack = output.stack();
            if (stack.isEmpty()) continue;
            var slot = builder.addOutputSlot(OUTPUT_X + i * OUTPUT_GAP, SLOT_Y).setOutputSlotBackground().add(stack);
            if (!output.guaranteed()) {
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "jei.jomender.crushing.chance", percent(output.chance())).withStyle(ChatFormatting.GOLD)));
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(recipe.time()).setPosition(26, SLOT_Y);
    }

    @Override
    public void draw(CrushingRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        List<CrushingRecipe.Output> outputs = recipe.outputs();
        for (int i = 0; i < outputs.size(); i++) {
            CrushingRecipe.Output output = outputs.get(i);
            if (!output.guaranteed()) {
                String text = percent(output.chance());
                int x = OUTPUT_X + i * OUTPUT_GAP + 9 - font.width(text) / 2;
                graphics.text(font, Component.literal(text), x, 24, 0xFFB07000, false);
            }
        }
        String info = String.format("%,d FE  •  %.1fs", recipe.energy(), recipe.time() / 20.0);
        graphics.text(font, Component.literal(info), 1, 36, 0xFF606060, false);

        Component source = sourceLabel(recipe.source());
        if (source != null) {
            graphics.text(font, source, 146 - font.width(source), 36, 0xFF8080A0, false);
        }
    }

    private static String percent(float chance) {
        return Math.round(chance * 100) + "%";
    }

    /** Woher das Rezept kommt: Tag-Automatik oder der Name der anderen Mod. */
    private static Component sourceLabel(String source) {
        if (source.isEmpty()) return null;
        if (source.equals(CrusherRecipes.SOURCE_TAGS)) return Component.translatable("jei.jomender.crushing.auto");
        int colon = source.indexOf(':');
        String mod = colon > 0 ? source.substring(0, colon) : source;
        return Component.literal(mod);
    }
}
