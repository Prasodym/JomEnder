package de.jomender.reactor;

import de.jomender.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menü des Reaktor-Controllers. Koordinaten siehe ReactorLayout. */
public class ReactorMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE = 0;

    // Slot-Positionen (müssen zum Screen passen)
    public static final int FUEL_X = 26, FUEL_Y = 30;
    public static final int WASTE_X = 26, WASTE_Y = 66;
    public static final int PLAYER_X = 9, PLAYER_Y = 108, HOTBAR_Y = 166;

    private final Container container;
    private final ContainerData data;
    private final BlockPos controllerPos;

    // Client
    public ReactorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(id, playerInventory, new SimpleContainer(2),
                new SimpleContainerData(ReactorControllerBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    // Server
    public ReactorMenu(int id, Inventory playerInventory, Container container, ContainerData data, BlockPos controllerPos) {
        super(ModMenuTypes.REACTOR_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;
        this.controllerPos = controllerPos;
        checkContainerSize(container, 2);
        checkContainerDataCount(data, ReactorControllerBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new Slot(container, ReactorControllerBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ReactorControllerBlockEntity.isFuel(stack);
            }
        });
        addSlot(new Slot(container, ReactorControllerBlockEntity.WASTE_SLOT, WASTE_X, WASTE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // nur herausnehmen
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_TOGGLE && container instanceof ReactorControllerBlockEntity controller) {
            controller.toggleActive();
            return true;
        }
        return false;
    }

    /** Shift-Klick: Uran in den Reaktor, Müll/Uran zurück ins Inventar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = 2, playerEnd = slots.size();

        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (ReactorControllerBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    // =========================
    // WERTE FÜR DEN SCREEN
    // =========================

    public BlockPos getControllerPos() { return controllerPos; }

    public int getEnergy() {
        return (data.get(ReactorControllerBlockEntity.DATA_ENERGY_LO) & 0xFFFF)
                | (data.get(ReactorControllerBlockEntity.DATA_ENERGY_HI) & 0xFFFF) << 16;
    }

    public int getHeat() { return data.get(ReactorControllerBlockEntity.DATA_HEAT) & 0xFFFF; }
    public int getWater() { return data.get(ReactorControllerBlockEntity.DATA_WATER) & 0xFFFF; }
    public int getFuel() { return data.get(ReactorControllerBlockEntity.DATA_FUEL) & 0xFFFF; }
    public int getFePerTick() { return data.get(ReactorControllerBlockEntity.DATA_FE_PER_TICK) & 0xFFFF; }
    public boolean isActive() { return data.get(ReactorControllerBlockEntity.DATA_ACTIVE) != 0; }

    public ReactorControllerBlockEntity.Status getStatus() {
        var values = ReactorControllerBlockEntity.Status.values();
        return values[Math.clamp(data.get(ReactorControllerBlockEntity.DATA_STATUS), 0, values.length - 1)];
    }

    public ReactorStructure.Problem getProblem() {
        var values = ReactorStructure.Problem.values();
        return values[Math.clamp(data.get(ReactorControllerBlockEntity.DATA_PROBLEM), 0, values.length - 1)];
    }

    /** {x, y, z} oder null, wenn nicht geformt. */
    public int[] getSize() {
        int packed = data.get(ReactorControllerBlockEntity.DATA_SIZE);
        if (packed == 0) return null;
        return new int[]{packed & 15, packed >> 4 & 15, packed >> 8 & 15};
    }

    /** Position des fehlerhaften Blocks oder null. */
    public BlockPos getProblemPos() {
        int packed = data.get(ReactorControllerBlockEntity.DATA_PROBLEM_POS) & 0xFFF;
        if (packed == 0) return null;
        return controllerPos.offset((packed & 15) - 8, (packed >> 4 & 15) - 8, (packed >> 8 & 15) - 8);
    }
}
