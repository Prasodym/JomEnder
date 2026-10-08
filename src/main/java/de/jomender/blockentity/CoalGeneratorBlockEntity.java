
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.ModDataComponents;
import de.jomender.menu.CoalGeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import de.jomender.ModEnergyTransfer;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class CoalGeneratorBlockEntity
        extends BlockEntity implements Container, MenuProvider {

    // =========================
    // ENERGIE
    // =========================

    public static final int MAX_ENERGY = 20000;
    public static final int ENERGY_PER_TICK = 40;

    /*
     * NeoForge-Energiespeicher
     *
     * Kapazität: 20.000 FE
     * Aufnahme: 0 FE
     * Entnahme: maximal 40 FE pro Vorgang
     *
     * Durch die Energy-Capability können später
     * andere Technik-Mods darauf zugreifen.
     */
    private final SimpleEnergyHandler energyStorage =
            new SimpleEnergyHandler(
                    MAX_ENERGY,
                    0,
                    ENERGY_PER_TICK
            ) {
                @Override
                protected void onEnergyChanged(int previousAmount) {
                    CoalGeneratorBlockEntity.this.setChanged();
                }
            };

    // =========================
    // BRENNSTOFF
    // =========================

    private int burnTime = 0;
    private int maxBurnTime = 0;

    // =========================
    // INVENTAR
    // =========================

    // Slot 0 = Kohle
    // Slots 1-4 = Upgrades
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(5, ItemStack.EMPTY);

    // =========================
    // GUI-DATEN
    // =========================

    public final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getEnergy();
                case 1 -> burnTime;
                case 2 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.set(
                        Math.clamp(value, 0, MAX_ENERGY)
                );
                case 1 -> burnTime = Math.max(0, value);
                case 2 -> maxBurnTime = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    // =========================
    // KONSTRUKTOR
    // =========================

    public CoalGeneratorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.COAL_GENERATOR.get(),
                pos,
                state
        );
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

    // =========================
    // BRENNSTOFF HINZUFÜGEN
    // =========================

    public boolean addFuel(ItemStack stack) {

        if (!isFuel(stack)) {
            return false;
        }

        ItemStack stored = items.get(0);

        if (!stored.isEmpty()
                && !ItemStack.isSameItemSameComponents(stored, stack)) {
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
        return stack.is(Items.COAL)
                || stack.is(Items.CHARCOAL);
    }

    // =========================
    // GENERATOR-TICK
    // =========================


    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CoalGeneratorBlockEntity generator
    ) {
        if (level.isClientSide()) {
            return;
        }

        // Zuerst Strom an benachbarte FE-Verbraucher abgeben.
        // Maximal 40 FE insgesamt pro Tick.
        ModEnergyTransfer.sendToNeighbors(
                level,
                pos,
                generator.getEnergyStorage(),
                ENERGY_PER_TICK
        );

        // Wenn danach immer noch voll:
        // Keine weitere Kohle verbrennen.
        if (generator.getEnergy() >= MAX_ENERGY) {
            return;
        }

        // Neue Kohle starten, falls nötig.
        if (generator.burnTime <= 0) {
            ItemStack fuel = generator.items.get(0);

            if (isFuel(fuel)) {
                fuel.shrink(1);

                generator.burnTime = 1600;
                generator.maxBurnTime = 1600;
                generator.setChanged();
            }
        }

        // Energie erzeugen.
        if (generator.burnTime > 0) {
            generator.burnTime--;

            generator.getEnergyStorage().set(
                    Math.min(
                            MAX_ENERGY,
                            generator.getEnergy() + ENERGY_PER_TICK
                    )
            );

            generator.setChanged();
        }
    }


    // =========================
    // STATUSWERTE
    // =========================

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getStoredCoal() {
        return items.get(0).getCount();
    }

    // =========================
    // INVENTAR-METHODEN
    // =========================

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

        // Upgrades kommen später.
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
    // GUI
    // =========================

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

    // =========================
    // INVENTAR BEIM ABBAUEN
    // =========================

    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state
    ) {
        /*
         * Inventar nicht einzeln auswerfen!
         *
         * Kohle und Upgrades bleiben über
         * minecraft:container im Generator-Item.
         *
         * Deshalb hier kein super-Aufruf.
         */
    }

    // =========================
    // ITEM-DATEN SAMMELN
    // =========================

    @Override
    protected void collectImplicitComponents(
            DataComponentMap.Builder components
    ) {

        super.collectImplicitComponents(components);

        // Gespeicherte FE
        components.set(
                ModDataComponents.GENERATOR_ENERGY.get(),
                getEnergy()
        );

        // Kohle und vier Upgrade-Slots
        components.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(items)
        );
    }

    // =========================
    // ITEM-DATEN WIEDERHERSTELLEN
    // =========================

    @Override
    protected void applyImplicitComponents(
            DataComponentGetter components
    ) {

        super.applyImplicitComponents(components);

        // FE wiederherstellen
        Integer savedEnergy = components.get(
                ModDataComponents.GENERATOR_ENERGY.get()
        );

        if (savedEnergy != null) {

            energyStorage.set(
                    Math.clamp(
                            savedEnergy,
                            0,
                            MAX_ENERGY
                    )
            );
        }

        // Inventar wiederherstellen
        ItemContainerContents savedItems =
                components.get(DataComponents.CONTAINER);

        if (savedItems != null) {
            savedItems.copyInto(items);
        }

        setChanged();
    }

    // =========================
    // WELT SPEICHERN
    // =========================

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);

        output.putInt("energy", getEnergy());
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurnTime", maxBurnTime);

        ContainerHelper.saveAllItems(output, items);
    }

    // =========================
    // WELT LADEN
    // =========================

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

        burnTime = Math.max(
                0,
                input.getIntOr("burnTime", 0)
        );

        maxBurnTime = Math.max(
                0,
                input.getIntOr("maxBurnTime", 0)
        );

        ContainerHelper.loadAllItems(input, items);

        // Kompatibilität mit alten Generator-Speicherdaten
        int oldCoal = input.getIntOr("storedCoal", 0);

        if (oldCoal > 0 && items.get(0).isEmpty()) {

            items.set(
                    0,
                    new ItemStack(
                            Items.COAL,
                            Math.min(oldCoal, 64)
                    )
            );
        }
    }
}
