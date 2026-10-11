package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.ArcFusionChamberBlockEntity;
import de.jomender.client.gui.ArcFusionLayout;
import de.jomender.sideconfig.SideConfigMenu;
import de.jomender.upgrade.Upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcFusionChamberMenu extends AbstractContainerMenu implements SideConfigMenu {

    private final Container inventory;
    private final ContainerData data;
    private final BlockPos machinePos;
    private boolean upgradePopupOpen;
    private int popupSlotX = ArcFusionLayout.UPGRADE_X;
    private int popupSlotY = ArcFusionLayout.UPGRADE_Y;

    // Client: Blockposition kommt vom Server mit (openMenu im Block).
    public ArcFusionChamberMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(id, playerInventory,
                new SimpleContainer(ArcFusionChamberBlockEntity.TOTAL_SLOTS),
                new SimpleContainerData(ArcFusionChamberBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    // Server
    public ArcFusionChamberMenu(int id, Inventory playerInventory,
                                Container inventory, ContainerData data, BlockPos machinePos) {
        super(ModMenuTypes.ARC_FUSION_CHAMBER.get(), id);
        this.inventory = inventory;
        this.data = data;
        this.machinePos = machinePos;

        checkContainerSize(inventory, ArcFusionChamberBlockEntity.TOTAL_SLOTS);
        checkContainerDataCount(data, ArcFusionChamberBlockEntity.DATA_COUNT);
        addDataSlots(data);

        // 2 Inputs (Slots 0-1)
        for (int i = 0; i < ArcFusionChamberBlockEntity.INPUT_COUNT; i++) {
            addSlot(new Slot(inventory, ArcFusionChamberBlockEntity.INPUT_START + i,
                    ArcFusionLayout.INPUT_X + i * ArcFusionLayout.SLOT_GAP, ArcFusionLayout.INPUT_Y));
        }

        // 2 Outputs (Slots 2-3), nur entnehmen
        for (int i = 0; i < ArcFusionChamberBlockEntity.OUTPUT_COUNT; i++) {
            addSlot(new Slot(inventory, ArcFusionChamberBlockEntity.OUTPUT_START + i,
                    ArcFusionLayout.OUTPUT_X + i * ArcFusionLayout.SLOT_GAP, ArcFusionLayout.OUTPUT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // 4 Upgrades (Slots 4-7), im Popup
        for (int i = 0; i < ArcFusionChamberBlockEntity.UPGRADE_COUNT; i++) {
            addSlot(createUpgradeSlot(ArcFusionChamberBlockEntity.UPGRADE_START + i,
                    ArcFusionLayout.UPGRADE_X + i * ArcFusionLayout.UPGRADE_GAP, ArcFusionLayout.UPGRADE_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        ArcFusionLayout.PLAYER_X + col * 18, ArcFusionLayout.PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    ArcFusionLayout.PLAYER_X + col * 18, ArcFusionLayout.HOTBAR_Y));
        }
    }

    private Slot createUpgradeSlot(int index, int x, int y) {
        // Nur Speed/Efficient, max. 8, jede Sorte nur einmal.
        return Upgrades.slot(inventory, index, x, y,
                ArcFusionChamberBlockEntity.UPGRADE_START, ArcFusionChamberBlockEntity.UPGRADE_COUNT,
                () -> upgradePopupOpen);
    }

    /** Clientseitig: Upgrade-Slots folgen dem verschiebbaren Popup. */
    public void moveUpgradeSlots(int x, int y) {
        if (popupSlotX == x && popupSlotY == y) return;
        popupSlotX = x;
        popupSlotY = y;
        for (int i = 0; i < ArcFusionChamberBlockEntity.UPGRADE_COUNT; i++) {
            int menuIndex = ArcFusionChamberBlockEntity.UPGRADE_START + i;
            Slot moved = createUpgradeSlot(menuIndex, x + i * ArcFusionLayout.UPGRADE_GAP, y);
            moved.index = menuIndex;
            slots.set(menuIndex, moved);
        }
    }

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
    public int getSideDataIndex() { return ArcFusionChamberBlockEntity.DATA_SIDE_CONFIG; }

    @Override
    public boolean isSideConfigurable(Direction side) {
        return ArcFusionChamberBlockEntity.isConfigurableSide(side);
    }

    @Override
    public Component getSideNote(Direction side) {
        return side == Direction.DOWN ? Component.literal("Energy input") : null;
    }

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
        // Shift-Klick folgt später (wie bei den anderen Maschinen)
        return ItemStack.EMPTY;
    }

    public int getEnergy() { return data.get(ArcFusionChamberBlockEntity.DATA_ENERGY); }
    public int getProgress() { return data.get(ArcFusionChamberBlockEntity.DATA_PROGRESS); }
    public int getMaxProgress() { return data.get(ArcFusionChamberBlockEntity.DATA_MAX_PROGRESS); }
    public int getEnergyPerTick() { return data.get(ArcFusionChamberBlockEntity.DATA_ENERGY_PER_TICK); }
}
