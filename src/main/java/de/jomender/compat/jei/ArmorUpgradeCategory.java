package de.jomender.compat.jei;

import de.jomender.ModDataComponents;
import de.jomender.ModItems;
import de.jomender.armor.ArmorUpgrade;
import de.jomender.armor.ArmorUpgrades;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** JEI-Seite "Armor Upgrades" - wird direkt aus {@link ArmorUpgrade} erzeugt. */
public class ArmorUpgradeCategory extends AbstractRecipeCategory<ArmorUpgrade> {
    public static final IRecipeType<ArmorUpgrade> TYPE = IRecipeType.create("jomender", "armor_upgrades", ArmorUpgrade.class);

    public ArmorUpgradeCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.jomender.armor_upgrades"),
                guiHelper.createDrawableItemLike(ModItems.ENDARIUM_CHESTPLATE.get()), 116, 44);
    }

    public static List<ArmorUpgrade> recipes() {
        return Arrays.asList(ArmorUpgrade.values());
    }

    private static Item armorFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ModItems.ENDARIUM_HELMET.get();
            case CHEST -> ModItems.ENDARIUM_CHESTPLATE.get();
            case LEGS -> ModItems.ENDARIUM_LEGGINGS.get();
            default -> ModItems.ENDARIUM_BOOTS.get();
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArmorUpgrade upgrade, IFocusGroup focuses) {
        // Passt das Upgrade in mehrere Teile, wechseln Ein- und Ausgabe gemeinsam durch
        List<ItemStack> armor = new ArrayList<>();
        List<ItemStack> upgraded = new ArrayList<>();
        for (EquipmentSlot slot : upgrade.slots()) {
            ItemStack stack = new ItemStack(armorFor(slot));
            armor.add(stack);
            ItemStack result = stack.copy();
            result.set(ModDataComponents.ARMOR_UPGRADES.get(), ArmorUpgrades.EMPTY.with(upgrade, 1));
            upgraded.add(result);
        }

        var input = builder.addInputSlot(1, 4).setStandardSlotBackground().addItemStacks(armor);
        builder.addInputSlot(21, 4).setStandardSlotBackground().add(new ItemStack(upgrade.item(), upgrade.cost()));
        var output = builder.addOutputSlot(89, 4).setOutputSlotBackground().addItemStacks(upgraded);
        builder.createFocusLink(input, output);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ArmorUpgrade upgrade, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(53, 4);
    }

    @Override
    public void draw(ArmorUpgrade upgrade, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, upgrade.displayName(), 1, 26, 0xFF7A3FC0, false);
        Component info = upgrade.stackable()
                ? Component.translatable("jei.jomender.armor_upgrades.stackable", upgrade.maxLevel())
                : Component.translatable("jei.jomender.armor_upgrades.once");
        graphics.text(font, info, 1, 36, 0xFF606060, false);
    }
}
