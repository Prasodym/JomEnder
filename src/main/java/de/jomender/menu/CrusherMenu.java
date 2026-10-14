
package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.CrusherBlockEntity;
import de.jomender.client.gui.JomEnderGuiLayout;
import de.jomender.sideconfig.SideConfigMenu;
import de.jomender.upgrade.Upgrades;

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

public class CrusherMenu extends AbstractContainerMenu implements SideConfigMenu {

    private final Container inventory;
    private final ContainerData data;
    private final BlockPos machinePos;
    private boolean upgradePopupOpen;
    private int popupSlotX = JomEnderGuiLayout.UPGRADE_X;
    private int popupSlotY = JomEnderGuiLayout.UPGRADE_Y;

    // Client-Konstruktor: die Blockposition kommt vom Server mit (siehe openMenu im Block).
    public CrusherMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                id,
                playerInventory,
                new SimpleContainer(
                        CrusherBlockEntity.TOTAL_SLOTS
                ),
                new SimpleContainerData(CrusherBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    // Server-Konstruktor
    public CrusherMenu(
            int id,
            Inventory playerInventory,
            Container inventory,
            ContainerData data,
            BlockPos machinePos
    ) {
        super(ModMenuTypes.CRUSHER.get(), id);

        this.inventory = inventory;
        this.data = data;
        this.machinePos = machinePos;

        checkContainerSize(
                inventory, CrusherBlockEntity.TOTAL_SLOTS
        );
        checkContainerDataCount(data, CrusherBlockEntity.DATA_COUNT);
        addDataSlots(data);

        // 3 input slots, IDs 0-2.
        for (int i = 0; i < CrusherBlockEntity.INPUT_COUNT; i++) {
            addSlot(new Slot(inventory, i,
                    JomEnderGuiLayout.INPUT_X + i * JomEnderGuiLayout.INPUT_GAP,
                    JomEnderGuiLayout.INPUT_Y));
        }

        // 8 output slots, IDs 3-10, four columns and two rows.
        for (int i = 0; i < 8; i++) {
            addSlot(new Slot(inventory, CrusherBlockEntity.OUTPUT_START + i,
                    JomEnderGuiLayout.OUTPUT_X + (i % 4) * JomEnderGuiLayout.OUTPUT_GAP,
                    JomEnderGuiLayout.OUTPUT_Y + (i / 4) * JomEnderGuiLayout.OUTPUT_GAP) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // Existing upgrade slots, now IDs 11-14.
        for (int i = 0; i < 4; i++) {
            addSlot(upgradeSlot(CrusherBlockEntity.UPGRADE_START + i,
                    JomEnderGuiLayout.UPGRADE_X + i * JomEnderGuiLayout.UPGRADE_GAP,
                    JomEnderGuiLayout.UPGRADE_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        JomEnderGuiLayout.PLAYER_X + col * 18,
                        JomEnderGuiLayout.PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    JomEnderGuiLayout.PLAYER_X + col * 18,
                    JomEnderGuiLayout.HOTBAR_Y));
        }
    }

    /**
     * Repositions the four client-side GUI slots to follow the draggable popup.
     * Slot x/y fields are final in Minecraft 26.1, so we replace only the client
     * presentation Slot objects; slot indices and underlying inventory stay unchanged.
     * Do not call this from server game logic.
     */
    public void moveUpgradeSlots(int x, int y) {
        if (popupSlotX == x && popupSlotY == y) return;
        popupSlotX = x;
        popupSlotY = y;
        for (int i = 0; i < 4; i++) {
            int menuIndex = CrusherBlockEntity.UPGRADE_START + i;
            Slot moved = upgradeSlot(menuIndex, x + i * JomEnderGuiLayout.UPGRADE_GAP, y);
            moved.index = menuIndex;
            slots.set(menuIndex, moved);
        }
    }

    /** Upgrade-Slot: nur Speed/Efficient, max. 8, jede Sorte nur einmal. */
    private Slot upgradeSlot(int index, int x, int y) {
        return Upgrades.slot(inventory, index, x, y,
                CrusherBlockEntity.UPGRADE_START, 4, () -> upgradePopupOpen);
    }

    /** Client-side visibility; does not change server-side inventory permissions. */
    public void setUpgradePopupOpen(boolean open) {
        this.upgradePopupOpen = open;
    }

    // =========================
    // SEITENKONFIGURATION
    // =========================

    @Override
    public BlockPos getMachinePos() { return machinePos; }

    @Override
    public ContainerData getSideData() { return data; }

    @Override
    public int getSideDataIndex() { return CrusherBlockEntity.DATA_SIDE_CONFIG; }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return handleSideButton(buttonId);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    /** Shift-Klick: Maschine -> Spielerinventar, Spielerinventar -> Eingänge (Upgrades -> Upgrade-Slots). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineEnd = CrusherBlockEntity.TOTAL_SLOTS;
        int playerEnd = machineEnd + 36;

        if (index < machineEnd) {
            if (!moveItemStackTo(stack, machineEnd, playerEnd, true)) return ItemStack.EMPTY;
        } else if (Upgrades.isUpgrade(stack)) {
            if (!moveItemStackTo(stack, CrusherBlockEntity.UPGRADE_START, CrusherBlockEntity.TOTAL_SLOTS, false)
                    && !moveItemStackTo(stack, 0, CrusherBlockEntity.INPUT_COUNT, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, CrusherBlockEntity.INPUT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getProgress() {
        return data.get(1);
    }

    public int getMaxProgress() {
        return data.get(2);
    }
}
