
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.menu.CoalGeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;


public class CoalGeneratorBlockEntity extends BlockEntity
        implements Container, MenuProvider {

    public static final int MAX_ENERGY = 20000;
    public static final int ENERGY_PER_TICK = 40;

    private int energy = 0;
    private int burnTime = 0;
    private int maxBurnTime = 0;


    public final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> burnTime;
                case 2 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = value;
                case 1 -> burnTime = value;
                case 2 -> maxBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };


    // 0 = Kohle, 1-4 = Upgrades
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(5, ItemStack.EMPTY);

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR.get(), pos, state);
    }

    // Brennstoff aus bisherigem Rechtsklick-System
    public boolean addFuel(ItemStack stack) {
        if (!isFuel(stack)) {
            return false;
        }

        ItemStack stored = items.get(0);

        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) {
            return false;
        }

        if (!stored.isEmpty() && stored.getCount() >= 64) {
            return false;
        }

        if (stored.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            setItem(0, copy);
        } else {
            stored.grow(1);
            setChanged();
        }

        return true;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    // Generator läuft serverseitig
    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CoalGeneratorBlockEntity generator
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (generator.energy >= MAX_ENERGY) {
            return;
        }

        if (generator.burnTime <= 0) {
            ItemStack fuel = generator.items.get(0);

            if (isFuel(fuel)) {
                fuel.shrink(1);
                generator.setChanged();

                generator.burnTime = 1600;
                generator.maxBurnTime = 1600;
            }
        }

        if (generator.burnTime > 0) {
            generator.burnTime--;

            generator.energy = Math.min(
                    MAX_ENERGY,
                    generator.energy + ENERGY_PER_TICK
            );

            generator.setChanged();
        }
    }

    public int getEnergy() {
        return energy;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getStoredCoal() {
        return items.get(0).getCount();
    }

    // Inventar
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
        if (slot == 0) {
            return isFuel(stack);
        }

        // Upgrades werden später freigeschaltet
        return false;
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
        items.clear();
        for (int i = 0; i < 5; i++) {
            items.add(ItemStack.EMPTY);
        }
        setChanged();
    }

    // Menü
    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.jomender.coal_generator"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory playerInventory,
            Player player
    ) {
        return new CoalGeneratorMenu(
                id,
                playerInventory,
                this,
                this.data
        );
    }





    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state
    ) {
        // Den Inhalt NICHT einzeln droppen.
        // Er soll zusammen mit der Energie
        // im Generator-Item gespeichert bleiben.

        // Hier absichtlich kein super-Aufruf.
    }





    // Speichern
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("energy", energy);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurnTime", maxBurnTime);

        ContainerHelper.saveAllItems(output, items);
    }

    // Laden
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        energy = Math.clamp(
                input.getIntOr("energy", 0), 0, MAX_ENERGY
        );

        burnTime = Math.max(
                0, input.getIntOr("burnTime", 0)
        );

        maxBurnTime = Math.max(
                0, input.getIntOr("maxBurnTime", 0)
        );

        ContainerHelper.loadAllItems(input, items);

        // Alten Kohlevorrat übernehmen
        int oldCoal = input.getIntOr("storedCoal", 0);

        if (oldCoal > 0 && items.get(0).isEmpty()) {
            items.set(0, new ItemStack(
                    Items.COAL,
                    Math.min(oldCoal, 64)
            ));
        }
    }
}
