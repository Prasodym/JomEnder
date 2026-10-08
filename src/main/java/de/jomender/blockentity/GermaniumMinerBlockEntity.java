
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import de.jomender.menu.GermaniumMinerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public class GermaniumMinerBlockEntity
        extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int MAX_ENERGY = 1_000_000;
    public static final int MAX_TRANSFER = 5_000;

    public static final int STORAGE_SLOTS = 27;
    public static final int UPGRADE_SLOTS = 4;

    public static final int FILTER_START = 31;
    public static final int FILTER_SLOTS = 6;

    public static final int TOTAL_SLOTS = 37;

    private static final int[] OUTPUT_SLOTS = createOutputSlots();
    private static final int[] NO_SLOTS = {};


    public final ContainerData menuData = new ContainerData() {

        @Override
        public int get(int index) {
            int energy = getEnergy();

            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> minY;
                case 3 -> maxY;
                case 4 ->
                        (running ? 1 : 0)
                                | (silkTouch ? 2 : 0)
                                | (autoPush ? 4 : 0);
                case 5 -> radius;
                default -> 0;
            };
        }

        public ItemStack getFilter(int filterIndex) {
            if (filterIndex < 0 || filterIndex >= FILTER_SLOTS) {
                return ItemStack.EMPTY;
            }

            return items.get(FILTER_START + filterIndex);
        }



        public boolean matchesAnyFilter(ItemStack candidate) {
            if (candidate.isEmpty()) {
                return false;
            }

            boolean hasFilter = false;

            for (int i = 0; i < FILTER_SLOTS; i++) {
                ItemStack filter = getFilter(i);

                if (filter.isEmpty()) {
                    continue;
                }

                hasFilter = true;

                if (ItemStack.isSameItemSameComponents(
                        filter,
                        candidate
                )) {
                    return true;
                }
            }

            // Ohne Filter vorerst nichts abbauen.
            return false;
        }

        public void setFilter(int filterIndex, ItemStack template) {
            if (filterIndex < 0 || filterIndex >= FILTER_SLOTS) {
                return;
            }

            ItemStack filter = template.isEmpty()
                    ? ItemStack.EMPTY
                    : template.copyWithCount(1);

            items.set(FILTER_START + filterIndex, filter);

            setChanged();
        }

        @Override
        public void set(int index, int value) {
            // Die Menüdaten werden vom Server erzeugt.
            // Der Client liest nur die synchronisierten Werte.
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public void setFilter(int filterIndex, ItemStack template) {
        if (filterIndex < 0 || filterIndex >= FILTER_SLOTS) {
            return;
        }

        ItemStack filter = template.isEmpty()
                ? ItemStack.EMPTY
                : template.copyWithCount(1);

        items.set(FILTER_START + filterIndex, filter);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Germanium Digital Miner");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player
    ) {
        return new GermaniumMinerMenu(
                id,
                inventory,
                this,
                menuData
        );
    }


    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private final SimpleEnergyHandler energyStorage =
            new SimpleEnergyHandler(
                    MAX_ENERGY,
                    MAX_TRANSFER,
                    0
            ) {
                @Override
                protected void onEnergyChanged(int previousAmount) {
                    GermaniumMinerBlockEntity.this.setChanged();
                }
            };

    // Pipez und andere Item-Rohre können aus dem Lager ziehen.
    private final ResourceHandler<ItemResource> outputHandler =
            new WorldlyContainerWrapper(this, Direction.DOWN);

    // Einstellungen für die spätere GUI.
    private int minY = -64;
    private int maxY = 64;
    private int radius = 32;

    private boolean running = false;
    private boolean silkTouch = false;
    private boolean autoPush = false;

    public GermaniumMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GERMANIUM_MINER.get(), pos, state);
    }

    private static int[] createOutputSlots() {
        int[] slots = new int[STORAGE_SLOTS];

        for (int i = 0; i < STORAGE_SLOTS; i++) {
            slots[i] = i;
        }

        return slots;
    }

    // =========================
    // ENERGIE
    // =========================

    public SimpleEnergyHandler getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergy() {
        return energyStorage.getAmountAsInt();
    }

    // =========================
    // EINSTELLUNGEN
    // =========================

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isSilkTouch() {
        return silkTouch;
    }

    public boolean isAutoPush() {
        return autoPush;
    }

    public void setHeightRange(int minimum, int maximum) {
        if (minimum > maximum) {
            return;
        }

        minY = Math.clamp(minimum, -64, 320);
        maxY = Math.clamp(maximum, minY, 320);
        setChanged();
    }

    public void setRunning(boolean value) {
        running = value;
        setChanged();
    }

    public void setSilkTouch(boolean value) {
        silkTouch = value;
        setChanged();
    }

    public void setAutoPush(boolean value) {
        autoPush = value;
        setChanged();
    }

    // =========================
    // ITEM-AUTOMATISIERUNG
    // =========================

    public ResourceHandler<ItemResource> getItemHandler(Direction side) {
        // Keine externen Inputs in das Lager:
        // Seitenzugriff dient ausschließlich der Entnahme.
        return side == null ? null : outputHandler;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot, ItemStack stack, Direction side
    ) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot, ItemStack stack, Direction side
    ) {
        return side == Direction.DOWN
                && slot >= 0
                && slot < STORAGE_SLOTS;
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
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Die normalen Ausgabeslots werden später nur
        // durch die Mining-Engine befüllt.
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
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        setChanged();
    }

    // =========================
    // SPEICHERN IN DER WELT
    // =========================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("energy", getEnergy());
        output.putInt("minY", minY);
        output.putInt("maxY", maxY);
        output.putInt("radius", radius);

        output.putBoolean("running", running);
        output.putBoolean("silkTouch", silkTouch);
        output.putBoolean("autoPush", autoPush);

        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        energyStorage.set(Math.clamp(
                input.getIntOr("energy", 0),
                0,
                MAX_ENERGY
        ));

        minY = Math.clamp(
                input.getIntOr("minY", -64), -64, 320
        );

        maxY = Math.clamp(
                input.getIntOr("maxY", 64), minY, 320
        );

        radius = Math.clamp(
                input.getIntOr("radius", 32), 1, 32
        );

        running = input.getBooleanOr("running", false);
        silkTouch = input.getBooleanOr("silkTouch", false);
        autoPush = input.getBooleanOr("autoPush", false);

        ContainerHelper.loadAllItems(input, items);
    }

    // =========================
    // SPEICHERN IM MINER-ITEM
    // =========================

    @Override
    protected void collectImplicitComponents(
            DataComponentMap.Builder builder
    ) {
        super.collectImplicitComponents(builder);

        // Lager + Upgrades im Vanilla-Container-Component.
        builder.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(items)
        );

        // FE und Einstellungen im Custom-Data-Component.
        CompoundTag data = new CompoundTag();

        data.putInt("energy", getEnergy());
        data.putInt("minY", minY);
        data.putInt("maxY", maxY);
        data.putInt("radius", radius);

        data.putBoolean("running", running);
        data.putBoolean("silkTouch", silkTouch);
        data.putBoolean("autoPush", autoPush);

        builder.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(data)
        );
    }

    @Override
    protected void applyImplicitComponents(
            DataComponentGetter components
    ) {
        super.applyImplicitComponents(components);

        ItemContainerContents contents =
                components.get(DataComponents.CONTAINER);

        if (contents != null) {
            contents.copyInto(items);
        }

        CustomData custom =
                components.get(DataComponents.CUSTOM_DATA);

        if (custom != null) {
            CompoundTag data = custom.copyTag();

            energyStorage.set(Math.clamp(
                    data.getIntOr("energy", 0),
                    0,
                    MAX_ENERGY
            ));

            minY = Math.clamp(
                    data.getIntOr("minY", -64), -64, 320
            );

            maxY = Math.clamp(
                    data.getIntOr("maxY", 64), minY, 320
            );

            radius = Math.clamp(
                    data.getIntOr("radius", 32), 1, 32
            );

            running = data.getBooleanOr("running", false);
            silkTouch = data.getBooleanOr("silkTouch", false);
            autoPush = data.getBooleanOr("autoPush", false);
        }

        setChanged();
    }
}
