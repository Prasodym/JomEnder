package de.jomender.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Die beiden Input-Slots der Arc Fusion Chamber. */
public record ArcFusionInput(ItemStack first, ItemStack second) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> first;
            case 1 -> second;
            default -> throw new IllegalArgumentException("ArcFusionInput hat nur 2 Slots: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
