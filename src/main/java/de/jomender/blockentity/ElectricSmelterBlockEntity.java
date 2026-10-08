
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.ModItems;
import de.jomender.menu.ElectricSmelterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public class ElectricSmelterBlockEntity
        extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int MAX_ENERGY = 20000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;

    // Inventar:
    // 0-2:  Input
    // 3-8:  Output
    // 9-12: Upgrades
    public static final int INPUT_COUNT = 3;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_END = 9;
    public static final int UPGRADE_START = 9;
    public static final int TOTAL_SLOTS = 13;

    private static final int[] TOP_SLOTS = {0, 1, 2};
    private static final int[] BOTTOM_SLOTS = {3, 4, 5, 6, 7, 8};
    private static final int[] NO_SLOTS = {};

    // FE aufnehmen, nicht abgeben.
    private final SimpleEnergyHandler energyStorage =
            new SimpleEnergyHandler(
                    MAX_ENERGY,
                    1000,
                    0
            ) {
                @Override
                protected void onEnergyChanged(int previousAmount) {
                    ElectricSmelterBlockEntity.this.setChanged();
                }
            };

    private int progress = 0;
    private int activeInput = -1;
    private int nextInput = 0;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // Beide Wrapper greifen auf dasselbe Inventar zu,
    // respektieren aber unterschiedliche Blockseiten.
    private final ResourceHandler<ItemResource> inputHandler =
            new WorldlyContainerWrapper(this, Direction.UP);

    private final ResourceHandler<ItemResource> outputHandler =
            new WorldlyContainerWrapper(this, Direction.DOWN);

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getEnergy();
                case 1 -> progress;
                case 2 -> MAX_PROGRESS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.set(
                        Math.clamp(value, 0, MAX_ENERGY)
                );
                case 1 -> progress = Math.clamp(
                        value, 0, MAX_PROGRESS
                );
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ElectricSmelterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(ModBlockEntities.ELECTRIC_SMELTER.get(), pos, state);
    }

    // =========================
    // FE-SCHNITTSTELLE
    // =========================

    public SimpleEnergyHandler getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergy() {
        return energyStorage.getAmountAsInt();
    }

    public int getProgress() {
        return progress;
    }

    public int receiveEnergy(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int accepted = Math.min(
                Math.min(amount, 1000),
                MAX_ENERGY - getEnergy()
        );

        if (accepted > 0) {
            energyStorage.set(getEnergy() + accepted);
        }

        return accepted;
    }

    // =========================
    // ITEM-CAPABILITY FÜR PIPEZ
    // =========================

    public ResourceHandler<ItemResource> getItemHandler(
            Direction side
    ) {
        if (side == Direction.UP) {
            return inputHandler;
        }

        if (side == Direction.DOWN) {
            return outputHandler;
        }

        // Seiten und unbekannte Richtung:
        // kein automatisierter Item-Zugriff.
        return null;
    }

    // =========================
    // SEITENABHÄNGIGE INVENTARREGELN
    // =========================

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return TOP_SLOTS;
        }

        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }

        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            ItemStack stack,
            Direction side
    ) {
        return side == Direction.UP
                && slot >= 0
                && slot < INPUT_COUNT;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            ItemStack stack,
            Direction side
    ) {
        return side == Direction.DOWN
                && slot >= OUTPUT_START
                && slot < OUTPUT_END;
    }

    // =========================
    // REZEPTE
    // =========================

    private static ItemStack getSmeltingResult(
            ServerLevel level,
            ItemStack input
    ) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Exklusives Rezept: kein Vanilla-Ofen!
        if (input.is(ModItems.GermaniumClumb.get())) {
            return new ItemStack(ModItems.Germanium.get());
        }

        SingleRecipeInput recipeInput =
                new SingleRecipeInput(input);

        return level.recipeAccess()
                .getRecipeFor(
                        RecipeType.SMELTING,
                        recipeInput,
                        level
                )
                .map(holder -> holder.value()
                        .assemble(recipeInput)
                        .copy())
                .orElse(ItemStack.EMPTY);
    }

    // =========================
    // OUTPUT-SLOTS
    // =========================

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack current = items.get(slot);

            if (current.isEmpty()) {
                if (result.getCount() <= getMaxStackSize(result)) {
                    return true;
                }
                continue;
            }

            if (ItemStack.isSameItemSameComponents(current, result)
                    && current.getCount() + result.getCount()
                    <= Math.min(
                    current.getMaxStackSize(),
                    getMaxStackSize(current)
            )) {
                return true;
            }
        }

        return false;
    }

    private void insertOutput(ItemStack result) {
        // Zuerst vorhandene passende Stapel auffüllen.
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack current = items.get(slot);

            if (!current.isEmpty()
                    && ItemStack.isSameItemSameComponents(current, result)
                    && current.getCount() + result.getCount()
                    <= Math.min(
                    current.getMaxStackSize(),
                    getMaxStackSize(current)
            )) {
                current.grow(result.getCount());
                return;
            }
        }

        // Sonst freien Slot verwenden.
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, result.copy());
                return;
            }
        }
    }

    private int findNextInput(ServerLevel level) {
        for (int offset = 0; offset < INPUT_COUNT; offset++) {
            int slot = (nextInput + offset) % INPUT_COUNT;

            ItemStack result =
                    getSmeltingResult(level, items.get(slot));

            if (canOutput(result)) {
                return slot;
            }
        }

        return -1;
    }

    // =========================
    // SCHMELZLOGIK
    // =========================

    public static void serverTick(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            ElectricSmelterBlockEntity smelter
    ) {
        int slot = smelter.activeInput;

        if (slot < 0
                || slot >= INPUT_COUNT
                || !smelter.canOutput(
                getSmeltingResult(
                        level,
                        smelter.items.get(slot)
                )
        )) {

            slot = smelter.findNextInput(level);

            if (slot != smelter.activeInput) {
                smelter.progress = 0;
            }

            smelter.activeInput = slot;
        }

        if (slot == -1) {
            if (smelter.progress != 0) {
                smelter.progress = 0;
                smelter.setChanged();
            }
            return;
        }

        if (smelter.getEnergy() < ENERGY_PER_TICK) {
            return;
        }

        ItemStack result =
                getSmeltingResult(level, smelter.items.get(slot));

        if (result.isEmpty() || !smelter.canOutput(result)) {
            return;
        }

        smelter.energyStorage.set(
                smelter.getEnergy() - ENERGY_PER_TICK
        );

        smelter.progress++;

        if (smelter.progress >= MAX_PROGRESS) {
            smelter.items.get(slot).shrink(1);
            smelter.insertOutput(result);

            smelter.progress = 0;
            smelter.nextInput = (slot + 1) % INPUT_COUNT;
            smelter.activeInput = -1;
        }

        smelter.setChanged();
    }

    // =========================
    // INVENTAR
    // =========================

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
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
            // Nur zurücksetzen, wenn der gerade
            // verarbeitete Input vollständig leer wird.
            if (slot == activeInput && items.get(slot).isEmpty()) {
                progress = 0;
                activeInput = -1;
            }

            setChanged();
        }

        return result;
    }


    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = items.get(slot);
        items.set(slot, ItemStack.EMPTY);

        if (slot < INPUT_COUNT) {
            progress = 0;
            activeInput = -1;
        }

        setChanged();
        return result;
    }


    @Override
    public void setItem(int slot, ItemStack stack) {

        ItemStack previous = items.get(slot);

        boolean activeInputChanged =
                slot == activeInput
                        && !ItemStack.isSameItemSameComponents(previous, stack);

        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));

        // Nur zurücksetzen, wenn das verarbeitete Item
        // tatsächlich durch ein anderes ersetzt wird.
        if (activeInputChanged) {
            progress = 0;
            activeInput = -1;
        }

        setChanged();
    }


    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < INPUT_COUNT;
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

        progress = 0;
        activeInput = -1;
        setChanged();
    }

    // =========================
    // GUI
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.jomender.electric_smelter"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory playerInventory,
            Player player
    ) {
        return new ElectricSmelterMenu(
                id,
                playerInventory,
                this,
                data
        );
    }

    // =========================
    // SPEICHERN
    // =========================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("energy", getEnergy());
        output.putInt("progress", progress);
        output.putInt("activeInput", activeInput);
        output.putInt("nextInput", nextInput);

        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        energyStorage.set(
                Math.clamp(
                        input.getIntOr("energy", 0),
                        0,
                        MAX_ENERGY
                )
        );

        progress = Math.clamp(
                input.getIntOr("progress", 0),
                0,
                MAX_PROGRESS
        );

        activeInput = Math.clamp(
                input.getIntOr("activeInput", -1),
                -1,
                INPUT_COUNT - 1
        );

        nextInput = Math.clamp(
                input.getIntOr("nextInput", 0),
                0,
                INPUT_COUNT - 1
        );

        ContainerHelper.loadAllItems(input, items);
    }
}
