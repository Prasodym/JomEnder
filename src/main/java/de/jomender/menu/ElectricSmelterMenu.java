
package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.ElectricSmelterBlockEntity;
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

public class ElectricSmelterMenu extends AbstractContainerMenu implements SideConfigMenu {

    private final Container inventory;
    private final ContainerData data;
    private final BlockPos machinePos;
    private boolean upgradePopupOpen;
    private int popupSlotX = JomEnderGuiLayout.UPGRADE_X;
    private int popupSlotY = JomEnderGuiLayout.UPGRADE_Y;

    // Client-Konstruktor: die Blockposition kommt vom Server mit (siehe openMenu im Block).
    public ElectricSmelterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                id,
                playerInventory,
                new SimpleContainer(
                        ElectricSmelterBlockEntity.TOTAL_SLOTS
                ),
                new SimpleContainerData(ElectricSmelterBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    // Server-Konstruktor
    public ElectricSmelterMenu(
            int id,
            Inventory playerInventory,
            Container inventory,
            ContainerData data,
            BlockPos machinePos
    ) {
        super(ModMenuTypes.ELECTRIC_SMELTER.get(), id);

        this.inventory = inventory;
        this.data = data;
        this.machinePos = machinePos;

        checkContainerSize(
                inventory, ElectricSmelterBlockEntity.TOTAL_SLOTS
        );
        checkContainerDataCount(data, ElectricSmelterBlockEntity.DATA_COUNT);
        addDataSlots(data);

        // 3 input slots, IDs 0-2.
        for (int i = 0; i < ElectricSmelterBlockEntity.INPUT_COUNT; i++) {
            addSlot(new Slot(inventory, i,
                    JomEnderGuiLayout.INPUT_X + i * JomEnderGuiLayout.INPUT_GAP,
                    JomEnderGuiLayout.INPUT_Y));
        }

        // 8 output slots, IDs 3-10, four columns and two rows.
        for (int i = 0; i < 8; i++) {
            addSlot(new Slot(inventory, ElectricSmelterBlockEntity.OUTPUT_START + i,
                    JomEnderGuiLayout.OUTPUT_X + (i % 4) * JomEnderGuiLayout.OUTPUT_GAP,
                    JomEnderGuiLayout.OUTPUT_Y + (i / 4) * JomEnderGuiLayout.OUTPUT_GAP) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // Existing upgrade slots, now IDs 11-14.
        for (int i = 0; i < 4; i++) {
            addSlot(upgradeSlot(ElectricSmelterBlockEntity.UPGRADE_START + i,
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
            int menuIndex = ElectricSmelterBlockEntity.UPGRADE_START + i;
            Slot moved = upgradeSlot(menuIndex, x + i * JomEnderGuiLayout.UPGRADE_GAP, y);
            moved.index = menuIndex;
            slots.set(menuIndex, moved);
        }
    }

    /** Upgrade-Slot: nur Speed/Efficient, max. 8, jede Sorte nur einmal. */
    private Slot upgradeSlot(int index, int x, int y) {
        return Upgrades.slot(inventory, index, x, y,
                ElectricSmelterBlockEntity.UPGRADE_START, 4, () -> upgradePopupOpen);
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
    public int getSideDataIndex() { return ElectricSmelterBlockEntity.DATA_SIDE_CONFIG; }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return handleSideButton(buttonId);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-Klick später ergänzen
        return ItemStack.EMPTY;
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
