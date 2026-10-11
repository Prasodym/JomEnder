package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.CoalGeneratorBlockEntity;
import de.jomender.client.gui.CoalGeneratorLayout;
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
import net.minecraft.world.item.Items;

public class CoalGeneratorMenu extends AbstractContainerMenu implements SideConfigMenu {

    public static final int FUEL_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_COUNT = 4;
    private static final int UPGRADE_GAP = 22;

    private final Container inventory;
    private final ContainerData data;
    private final BlockPos machinePos;
    private boolean upgradePopupOpen;
    private int popupSlotX = CoalGeneratorLayout.UPGRADE_X;
    private int popupSlotY = CoalGeneratorLayout.UPGRADE_Y;

    // Client: Platzhalter für 5 Slots und die Datenwerte; die Blockposition kommt vom Server mit.
    public CoalGeneratorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(id, playerInventory, new SimpleContainer(5),
                new SimpleContainerData(CoalGeneratorBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    // Server: echtes Inventar und echte Energiedaten
    public CoalGeneratorMenu(int id, Inventory playerInventory,
                             Container inventory, ContainerData data, BlockPos machinePos) {
        super(ModMenuTypes.COAL_GENERATOR.get(), id);

        this.inventory = inventory;
        this.data = data;
        this.machinePos = machinePos;

        checkContainerSize(inventory, 5);
        checkContainerDataCount(data, CoalGeneratorBlockEntity.DATA_COUNT);
        addDataSlots(data);

        // Slot 0: Brennstoff
        addSlot(new Slot(inventory, FUEL_SLOT,
                CoalGeneratorLayout.FUEL_X, CoalGeneratorLayout.FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            }
        });

        // Slots 1-4: Upgrades, nur sichtbar wenn das Upgrade-Popup offen ist
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            addSlot(createUpgradeSlot(UPGRADE_START + i,
                    CoalGeneratorLayout.UPGRADE_X + i * UPGRADE_GAP,
                    CoalGeneratorLayout.UPGRADE_Y));
        }

        // Spielerinventar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        CoalGeneratorLayout.PLAYER_X + col * 18,
                        CoalGeneratorLayout.PLAYER_Y + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    CoalGeneratorLayout.PLAYER_X + col * 18,
                    CoalGeneratorLayout.HOTBAR_Y));
        }
    }

    private Slot createUpgradeSlot(int index, int x, int y) {
        // Nur Speed/Efficient, max. 8, jede Sorte nur einmal.
        return Upgrades.slot(inventory, index, x, y, UPGRADE_START, UPGRADE_COUNT, () -> upgradePopupOpen);
    }

    /**
     * Verschiebt die Upgrade-Slots clientseitig mit dem verschiebbaren Popup
     * (Slot-x/y sind final, daher werden die Slot-Objekte ersetzt).
     * Nicht aus Server-Logik aufrufen.
     */
    public void moveUpgradeSlots(int x, int y) {
        if (popupSlotX == x && popupSlotY == y) return;
        popupSlotX = x;
        popupSlotY = y;
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            int menuIndex = UPGRADE_START + i;
            Slot moved = createUpgradeSlot(menuIndex, x + i * UPGRADE_GAP, y);
            moved.index = menuIndex;
            slots.set(menuIndex, moved);
        }
    }

    /** Nur clientseitige Sichtbarkeit. */
    public void setUpgradePopupOpen(boolean open) {
        this.upgradePopupOpen = open;
    }

    // Seitenkonfiguration
    @Override
    public BlockPos getMachinePos() { return machinePos; }

    @Override
    public ContainerData getSideData() { return data; }

    @Override
    public int getSideDataIndex() { return CoalGeneratorBlockEntity.DATA_SIDE_CONFIG; }

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
        // Shift-Klick bauen wir später sauber ein
        return ItemStack.EMPTY;
    }

    public int getEnergy() { return data.get(0); }
    public int getBurnTime() { return data.get(1); }
    public int getMaxBurnTime() { return data.get(2); }
    public int getGenerationPerTick() { return data.get(CoalGeneratorBlockEntity.DATA_GENERATION); }
}
