package de.jomender.compat.jei;

import de.jomender.ModBlocks;
import de.jomender.ModItems;
import de.jomender.blockentity.ElectricSmelterBlockEntity;
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

/**
 * Rezepte, die NUR der Electric Smelter kann (kein Vanilla-Ofen).
 * Alle normalen Ofenrezepte zeigt JEI ohnehin - dort ist der Smelter als Station eingetragen.
 */
public class SmelterExclusiveCategory extends AbstractRecipeCategory<SmelterExclusiveCategory.Entry> {

    public record Entry(ItemStack input, ItemStack output) {}

    public static final IRecipeType<Entry> TYPE =
            IRecipeType.create("jomender", "electric_smelter_exclusive", Entry.class);

    /** Muss zu ElectricSmelterBlockEntity#getSmeltingResult passen. */
    public static List<Entry> recipes() {
        return List.of(
                new Entry(new ItemStack(ModItems.GermaniumClump.get()), new ItemStack(ModItems.Germanium.get()))
        );
    }

    public final IGuiHelper guiHelper;

    public SmelterExclusiveCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("block.jomender.electric_smelter"),
                guiHelper.createDrawableItemLike(ModBlocks.ELECTRIC_SMELTER.get()), 90, 40);
        this.guiHelper = guiHelper;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 4).setStandardSlotBackground().add(recipe.input());
        builder.addOutputSlot(63, 4).setOutputSlotBackground().add(recipe.output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Entry recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(ElectricSmelterBlockEntity.MAX_PROGRESS).setPosition(27, 4);
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        int energy = ElectricSmelterBlockEntity.MAX_PROGRESS * ElectricSmelterBlockEntity.ENERGY_PER_TICK;
        String info = String.format("%,d FE  •  %.1fs", energy, ElectricSmelterBlockEntity.MAX_PROGRESS / 20.0);
        graphics.text(Minecraft.getInstance().font, Component.literal(info), 1, 29, 0xFF606060, false);
    }
}
