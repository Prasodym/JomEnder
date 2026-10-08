
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ElectricSmelterBlockEntity
        extends BlockEntity implements Container {

    public static final int MAX_ENERGY = 20000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;

    private int energy = 0;
    private int progress = 0;

    // 0 = Input
    // 1 = Output
    // 2-5 = Upgrades
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(6, ItemStack.EMPTY);

    public ElectricSmelterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.ELECTRIC_SMELTER.get(),
                pos,
                state
        );
    }

    public int getEnergy() {
        return energy;
    }

    public int getProgress() {
        return progress;
    }

    public int receiveEnergy(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int accepted = Math.min(
                amount,
                MAX_ENERGY - energy
        );

        energy += accepted;

        if (accepted > 0) {
            setChanged();
        }

        return accepted;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result =
                ContainerHelper.removeItem(items, slot, amount);

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Output darf nicht manuell befüllt werden.
        // Upgrades schalten wir später frei.
        return slot == 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("energy", energy);
        output.putInt("progress", progress);

        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        energy = Math.clamp(
                input.getIntOr("energy", 0),
                0,
                MAX_ENERGY
        );

        progress = Math.clamp(
                input.getIntOr("progress", 0),
                0,
                MAX_PROGRESS
        );

        ContainerHelper.loadAllItems(input, items);
    }
}
