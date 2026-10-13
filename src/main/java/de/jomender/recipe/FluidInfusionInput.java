package de.jomender.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/** Alles, was gerade im selben Flüssigkeitsblock liegt: die Flüssigkeit und die Items darin. */
public record FluidInfusionInput(Fluid fluid, List<ItemStack> items) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
