
package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.GermaniumMinerBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GermaniumMinerMenu extends AbstractContainerMenu {



    // =========================
    // BUTTON-IDs
    // =========================

    public static final int BUTTON_START = 0;
    public static final int BUTTON_SILK = 1;
    public static final int BUTTON_PUSH = 2;
    public static final int BUTTON_FILTER_PAGE = 7;

    // Falls dein Screen diese Konstanten noch
    // verwendet, bleiben sie vorhanden.
    public static final int MIN_Y_BASE = 1000;
    public static final int MAX_Y_BASE = 2000;

    private static final int WORLD_MIN = -64;
    private static final int WORLD_MAX = 320;
    private static final int HEIGHT_RANGE = 385;
    private static final int HEIGHT_BUTTON_BASE = 3000;
    public static final int DATA_COUNT = 12;

    private final Container container;
    private final SimpleContainer ghostContainer;
    private final ContainerData data;
    private final GermaniumMinerBlockEntity miner;

    private boolean filterPage = false;

    // =========================
    // SEITEN
    // =========================

    public boolean isFilterPage() {
        return filterPage;
    }

    public void setFilterPage(boolean value) {
        filterPage = value;
    }

    // =========================
    // CLIENT-KONSTRUKTOR
    // =========================

    public GermaniumMinerMenu(int id, Inventory inventory) {
        this(
                id,
                inventory,
                new SimpleContainer(
                        GermaniumMinerBlockEntity.TOTAL_SLOTS
                ),
                new SimpleContainerData(DATA_COUNT),
                null
        );
    }

    // =========================
    // SERVER-KONSTRUKTOR
    // =========================

    public GermaniumMinerMenu(
            int id,
            Inventory inventory,
            GermaniumMinerBlockEntity miner,
            ContainerData data
    ) {
        this(id, inventory, miner, data, miner);
    }

    // =========================
    // GEMEINSAMER KONSTRUKTOR
    // =========================

    public int getFoundOres() {
        return (data.get(6) & 0xFFFF)
                | ((data.get(7) & 0xFFFF) << 16);
    }

    public int getMinedOres() {
        return (data.get(8) & 0xFFFF)
                | ((data.get(9) & 0xFFFF) << 16);
    }

    public int getScanProgress() {
        return data.get(10);
    }

    public boolean isCountScanComplete() {
        return (data.get(11) & 1) != 0;
    }

    public boolean hasUnloadedScanChunks() {
        return (data.get(11) & 2) != 0;
    }

    private GermaniumMinerMenu(
            int id,
            Inventory inventory,
            Container container,
            ContainerData data,
            GermaniumMinerBlockEntity miner
    ) {
        super(ModMenuTypes.GERMANIUM_MINER.get(), id);

        this.container = container;
        this.ghostContainer = new SimpleContainer(GermaniumMinerBlockEntity.FILTER_SLOTS);
        this.data = data;
        this.miner = miner;

        checkContainerSize(
                container,
                GermaniumMinerBlockEntity.TOTAL_SLOTS
        );

        checkContainerDataCount(data, DATA_COUNT);

        container.startOpen(inventory.player);
        addDataSlots(data);
        if (miner != null) {
            for (int i = 0; i < GermaniumMinerBlockEntity.FILTER_SLOTS; i++) {
                ghostContainer.setItem(i, miner.getFilter(i).copy());
            }
        }

        // =========================
        // 27 LAGERPLÄTZE
        // =========================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {

                int index = row * 9 + col;

                addSlot(new Slot(
                        container,
                        index,
                        23 + col * 18,
                        108 + row * 18
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean isActive() {
                        return !filterPage;
                    }
                });
            }
        }

        // =========================
        // 4 UPGRADE-SLOTS
        // =========================

        for (int i = 0; i < 4; i++) {

            addSlot(new Slot(
                    container,
                    27 + i,
                    207,
                    108 + i * 18
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return !filterPage;
                }
            });
        }



        // =========================
        // 6 GHOST-FILTER
        // =========================
        // Wichtig:
        // Die Filter werden NACH den 31 Maschinen-Slots
        // registriert, aber VOR dem Spielerinventar.
        //
        // Menü-Slot-IDs:
        // 0-26  = Lager
        // 27-30 = Upgrades
        // 31-36 = Ghost-Filter

        for (int i = 0;
             i < GermaniumMinerBlockEntity.FILTER_SLOTS;
             i++) {

            addSlot(new Slot(
                    ghostContainer,
                    i,
                    23 + i * 22,
                    119
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return filterPage;
                }
            });
        }

        // =========================
        // SPIELERINVENTAR
        // =========================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {

                addSlot(new Slot(
                        inventory,
                        col + row * 9 + 9,
                        23 + col * 18,
                        181 + row * 18
                ));
            }
        }

        // =========================
        // HOTBAR
        // =========================

        for (int col = 0; col < 9; col++) {

            addSlot(new Slot(
                    inventory,
                    col,
                    23 + col * 18,
                    241
            ));
        }
    }

    // =========================
    // GHOST-SLOT-KLICKS
    // =========================

    @Override
    public void clicked(
            int slotId,
            int mouseButton,
            ContainerInput containerInput,
            Player player
    ) {
        int firstFilterSlot =
                GermaniumMinerBlockEntity.FILTER_START;

        int lastFilterSlot =
                firstFilterSlot
                        + GermaniumMinerBlockEntity.FILTER_SLOTS;

        if (slotId >= firstFilterSlot
                && slotId < lastFilterSlot) {

            // Alle anderen Klickarten blockieren:
            // Shift-Klick, Hotbar-Tausch, Werfen usw.
            if (containerInput != ContainerInput.PICKUP
                    || !filterPage) {
                return;
            }

            int filterIndex = slotId - firstFilterSlot;

            // Nur der Server verändert die echten
            // Filter in der BlockEntity.
            if (miner != null && miner.stillValid(player)) {

                // ItemStack wird in setFilter() kopiert.
                // Der Mauszeiger verliert kein Item.
                miner.setFilter(filterIndex, getCarried());
                ghostContainer.setItem(filterIndex, miner.getFilter(filterIndex).copy());
                broadcastChanges();
            }

            return;
        }

        super.clicked(
                slotId,
                mouseButton,
                containerInput,
                player
        );
    }

    // =========================
    // GUI-DATEN
    // =========================

    public int getEnergy() {
        return (data.get(0) & 0xFFFF)
                | ((data.get(1) & 0xFFFF) << 16);
    }

    public int getMinY() {
        return data.get(2);
    }

    public int getMaxY() {
        return data.get(3);
    }

    public boolean isRunning() {
        return (data.get(4) & 1) != 0;
    }

    public boolean isSilkTouch() {
        return (data.get(4) & 2) != 0;
    }

    public boolean isAutoPush() {
        return (data.get(4) & 4) != 0;
    }

    public int getRadius() {
        return data.get(5);
    }

    // =========================
    // GUI-BUTTONS
    // =========================

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        if (miner == null || !miner.stillValid(player)) {
            return false;
        }

        // Min Y und Max Y gemeinsam übertragen.
        int maximumButtonId =
                HEIGHT_BUTTON_BASE
                        + HEIGHT_RANGE * HEIGHT_RANGE - 1;

        if (buttonId >= HEIGHT_BUTTON_BASE
                && buttonId <= maximumButtonId) {

            int encoded = buttonId - HEIGHT_BUTTON_BASE;

            int minimum =
                    encoded / HEIGHT_RANGE + WORLD_MIN;

            int maximum =
                    encoded % HEIGHT_RANGE + WORLD_MIN;

            if (minimum < WORLD_MIN
                    || minimum > WORLD_MAX
                    || maximum < WORLD_MIN
                    || maximum > WORLD_MAX
                    || minimum > maximum) {
                return false;
            }

            miner.setHeightRange(minimum, maximum);

            return true;
        }

        switch (buttonId) {

            case BUTTON_FILTER_PAGE ->
                    filterPage = !filterPage;

            case BUTTON_START ->
                    miner.setRunning(!miner.isRunning());

            case BUTTON_SILK ->
                    miner.setSilkTouch(!miner.isSilkTouch());

            case BUTTON_PUSH ->
                    miner.setAutoPush(!miner.isAutoPush());

            default -> {
                return false;
            }
        }

        return true;
    }

    // =========================
    // SHIFT-KLICK
    // =========================

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int slotIndex
    ) {
        // Bewusst gesperrt, bis die normale
        // Inventarlogik vollständig steht.
        return ItemStack.EMPTY;
    }

    // =========================
    // GÜLTIGKEIT
    // =========================

    @Override
    public boolean stillValid(Player player) {
        return miner == null || miner.stillValid(player);
    }

    // =========================
    // SCHLIESSEN
    // =========================

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
