package de.jomender.compat.jei;

import de.jomender.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * JEI-Seite "Mob Drops": zeigt, welches Mob welches Item mit welcher Chance fallen lässt.
 * Nur Anzeige - der echte Drop steht in data/jomender/loot_table/entities/*.json.
 * Ändert man dort die Chance, hier bei recipes() mit anpassen.
 */
public class MobDropCategory extends AbstractRecipeCategory<MobDropCategory.MobDrop> {
    public static final IRecipeType<MobDrop> TYPE = IRecipeType.create("jomender", "mob_drops", MobDrop.class);

    /** mob = Item, das das Mob darstellt (Spawn-Ei), chance = 0.0 bis 1.0 */
    public record MobDrop(ItemStack mob, Component mobName, ItemStack drop, float chance) {}

    public static List<MobDrop> recipes() {
        return List.of(
                new MobDrop(new ItemStack(Items.WITHER_SPAWN_EGG), EntityType.WITHER.getDescription(),
                        new ItemStack(ModItems.WEAPON_SCRAP.get()), 0.5F),
                // Galaxius: 1-3 Galaxis-Quarz (siehe loot_table/entities/galaxius.json)
                new MobDrop(new ItemStack(Items.ZOMBIE_SPAWN_EGG), de.jomender.ModEntities.GALAXIUS.get().getDescription(),
                        new ItemStack(ModItems.Galaxis_Quarz.get(), 2), 1.0F)
        );
    }

    public MobDropCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.jomender.mob_drops"),
                guiHelper.createDrawableItemLike(Items.WITHER_SKELETON_SKULL), 90, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobDrop recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 1, 4)
                .setStandardSlotBackground()
                .add(recipe.mob());
        builder.addOutputSlot(63, 4)
                .setOutputSlotBackground()
                .add(recipe.drop())
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(chanceText(recipe).withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MobDrop recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(27, 4);
    }

    @Override
    public void draw(MobDrop recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Component line = recipe.mobName().copy().append("  •  ").append(percent(recipe) + "%");
        graphics.text(Minecraft.getInstance().font, line, 1, 29, 0xFF606060, false);
    }

    private static net.minecraft.network.chat.MutableComponent chanceText(MobDrop recipe) {
        return Component.translatable("jei.jomender.mob_drops.chance", percent(recipe));
    }

    private static String percent(MobDrop recipe) {
        float p = recipe.chance() * 100F;
        return p == Math.floor(p) ? String.valueOf((int) p) : String.format("%.1f", p);
    }
}
