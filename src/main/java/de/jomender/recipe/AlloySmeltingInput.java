package de.jomender.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Die drei Eingangsslots des Electric Smelters. */
public record AlloySmeltingInput(ItemStack a, ItemStack b, ItemStack c) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> throw new IllegalArgumentException("Electric Smelter hat nur 3 Eingangsslots: " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
