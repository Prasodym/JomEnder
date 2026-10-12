package de.jomender.reactor;

import de.jomender.ModBlockEntities;
import de.jomender.ModItems;
import de.jomender.upgrade.MachineEnergyHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Herz des Reaktors.
 *
 * Ablauf pro Tick (nur wenn der Multiblock steht und eingeschaltet ist):
 *  1. Uran aus dem Brennstoff-Slot in den internen Brennstoff-Vorrat nachladen
 *  2. Brennstoff verbrennen  -> Strom (FE) + Hitze + nach und nach Atommüll
 *  3. Wasser aus dem Tank kühlt die Hitze wieder herunter
 *  4. Erreicht die Hitze das Maximum: Notabschaltung, bis er auf 30 % abgekühlt ist
 *
 * Die Leistung hängt am Innenraum: 3x3x3 = 1 Block, 7x7x7 = 125 Blöcke.
 */
public class ReactorControllerBlockEntity extends BlockEntity implements Container, MenuProvider {

    // =========================
    // BALANCING - hier kannst du drehen
    // =========================

    /** Brennstoff-Einheiten pro Uranbarren. */
    public static final int FUEL_PER_INGOT = 2000;
    /** Chance, dass pro verbranntem Uranbarren 1 Atommüll entsteht (0.10 = 10 %). */
    public static final float WASTE_CHANCE = 0.10F;
    /** Verbrauchte Brennstoff-Einheiten pro Tick und Innenraum-Block. */
    public static final int FUEL_PER_TICK_PER_BLOCK = 1;
    /** FE pro verbrannter Brennstoff-Einheit (ein Barren = 200.000 FE). */
    public static final int FE_PER_FUEL = 100;
    /** Hitze pro verbrannter Brennstoff-Einheit. */
    public static final int HEAT_PER_FUEL = 8;
    /** Kühlleistung: so viel Hitze nimmt 1 mB Wasser auf (-> 4 mB Wasser pro Brennstoff-Einheit). */
    public static final int HEAT_PER_WATER = 2;
    /** Kühlt auch ohne Wasser langsam ab. */
    public static final int PASSIVE_COOLING = 5;
    /** Notabschaltung bei 1000 °C (intern x10). */
    public static final int MAX_HEAT = 10_000;
    /** Wieder an bei 30 %. */
    public static final int RESTART_HEAT = MAX_HEAT * 3 / 10;

    public static final int ENERGY_CAPACITY = 5_000_000;
    public static final int WATER_CAPACITY = 32_000;

    // =========================
    // INVENTAR
    // =========================

    public static final int FUEL_SLOT = 0;
    public static final int WASTE_SLOT = 1;
    public static final TagKey<Item> URANIUM_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/uranium"));

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    private final MachineEnergyHandler energy =
            new MachineEnergyHandler(ENERGY_CAPACITY, 0, ReactorPortBlockEntity.MAX_ENERGY_PUSH, this::setChanged);

    private final WaterTank water = new WaterTank();

    /** Pipe-Zugriff über den Anschluss: Uran nur hinein, Müll nur heraus. */
    private final ResourceHandler<ItemResource> fuelInput =
            new WorldlyContainerWrapper(new SlotView(FUEL_SLOT, true, false), Direction.UP);
    private final ResourceHandler<ItemResource> wasteOutput =
            new WorldlyContainerWrapper(new SlotView(WASTE_SLOT, false, true), Direction.UP);

    // =========================
    // ZUSTAND
    // =========================

    public enum Status { NOT_FORMED, OFF, RUNNING, NO_FUEL, ENERGY_FULL, WASTE_FULL, SCRAM }

    private int fuel;            // interner Brennstoff-Vorrat
    private int wasteProgress;   // verbrannte Einheiten seit dem letzten Müll-Item
    private int heat;
    private boolean active = true;
    private boolean scram;
    private Status status = Status.NOT_FORMED;
    private int lastFePerTick;

    // Struktur (wird jede Sekunde neu geprüft)
    private boolean needsCheck = true;
    private ReactorStructure.Result structure = ReactorStructure.Result.fail(ReactorStructure.Problem.NO_INTERIOR, null);
    private final Set<BlockPos> ports = new HashSet<>();

    // Box, deren Blöcke gerade auf "formed" geschaltet sind (für die verbundene Optik)
    private @Nullable BlockPos shellMin, shellMax;

    // Nur für den Client (Wasser-Anzeige): Box und Füllstand aus dem letzten Sync
    private @Nullable BlockPos renderMin, renderMax;
    private int renderWater;
    private int lastSyncedWater = -1;
    private boolean lastSyncedFormed;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER.get(), pos, state);
    }

    // =========================
    // TICK
    // =========================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorControllerBlockEntity reactor) {
        if (reactor.needsCheck || level.getGameTime() % 20 == Math.floorMod(pos.hashCode(), 20)) {
            reactor.needsCheck = false;
            reactor.revalidate();
        }
        reactor.tickReactor();
    }

    private void tickReactor() {
        int produced = 0;

        if (!isFormed()) {
            status = Status.NOT_FORMED;
        } else if (scram) {
            status = Status.SCRAM;
        } else if (!active) {
            status = Status.OFF;
        } else {
            int burn = structure.interiorVolume() * FUEL_PER_TICK_PER_BLOCK;
            int fePerTick = burn * FE_PER_FUEL;

            if (energy.getAmountAsInt() + fePerTick > ENERGY_CAPACITY) {
                status = Status.ENERGY_FULL;
            } else {
                refuel(burn);
                if (fuel < burn) {
                    status = Status.NO_FUEL;
                } else if (wasteProgress + burn >= FUEL_PER_INGOT && !canStoreWaste()) {
                    status = Status.WASTE_FULL;
                } else {
                    fuel -= burn;
                    wasteProgress += burn;
                    while (wasteProgress >= FUEL_PER_INGOT) {
                        wasteProgress -= FUEL_PER_INGOT;
                        if (level.getRandom().nextFloat() < WASTE_CHANCE) {
                            addWaste();
                        }
                    }
                    energy.set(energy.getAmountAsInt() + fePerTick);
                    heat += burn * HEAT_PER_FUEL;
                    produced = fePerTick;
                    status = Status.RUNNING;
                }
            }
        }

        // Kühlen: erst passiv, dann mit Wasser
        heat = Math.max(0, heat - PASSIVE_COOLING);
        int storedWater = water.getAmountAsInt(0);
        if (heat > 0 && storedWater > 0) {
            int use = Math.min(storedWater, (heat + HEAT_PER_WATER - 1) / HEAT_PER_WATER);
            water.setWater(storedWater - use);
            heat = Math.max(0, heat - use * HEAT_PER_WATER);
        }

        // Notabschaltung
        if (heat >= MAX_HEAT) {
            heat = MAX_HEAT;
            scram = true;
        } else if (scram && heat <= RESTART_HEAT) {
            scram = false;
        }

        if (produced != lastFePerTick) {
            lastFePerTick = produced;
        }
        setChanged();
        syncToClientIfNeeded();
    }

    /** Wasserstand/Form an den Client schicken - nur bei sichtbarer Änderung, max. alle 5 Ticks. */
    private void syncToClientIfNeeded() {
        if (level == null || level.getGameTime() % 5 != 0) return;
        int waterNow = water.getAmountAsInt(0);
        boolean waterChanged = Math.abs(waterNow - lastSyncedWater) >= WATER_CAPACITY / 200
                || (waterNow == 0) != (lastSyncedWater == 0);
        if (waterChanged || isFormed() != lastSyncedFormed) {
            lastSyncedWater = waterNow;
            lastSyncedFormed = isFormed();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void refuel(int needed) {
        ItemStack stack = items.get(FUEL_SLOT);
        while (fuel < needed && isFuel(stack)) {
            stack.shrink(1);
            fuel += FUEL_PER_INGOT;
        }
    }

    private boolean canStoreWaste() {
        ItemStack stack = items.get(WASTE_SLOT);
        return stack.isEmpty() || (stack.is(ModItems.NUCLEAR_WASTE.get()) && stack.getCount() < stack.getMaxStackSize());
    }

    private void addWaste() {
        ItemStack stack = items.get(WASTE_SLOT);
        if (stack.isEmpty()) {
            items.set(WASTE_SLOT, new ItemStack(ModItems.NUCLEAR_WASTE.get()));
        } else if (canStoreWaste()) {
            stack.grow(1);
        }
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(URANIUM_INGOTS) || stack.is(ModItems.Uranium_Ingot.get());
    }

    // =========================
    // STRUKTUR
    // =========================

    private void revalidate() {
        if (level == null) return;
        structure = ReactorStructure.check(level, worldPosition);

        Set<BlockPos> newPorts = structure.formed() ? new HashSet<>(structure.ports()) : Set.of();
        for (BlockPos old : List.copyOf(ports)) {
            if (!newPorts.contains(old) && level.getBlockEntity(old) instanceof ReactorPortBlockEntity port) {
                port.setController(null);
            }
        }
        ports.clear();
        ports.addAll(newPorts);
        for (BlockPos portPos : ports) {
            if (level.getBlockEntity(portPos) instanceof ReactorPortBlockEntity port) {
                port.setController(worldPosition);
            }
        }

        // Optik: alte Hülle zurücksetzen, neue verbinden
        BlockPos newMin = structure.formed() ? structure.min() : null;
        BlockPos newMax = structure.formed() ? structure.max() : null;
        if (shellMin != null && shellMax != null
                && (newMin == null || !shellMin.equals(newMin) || !shellMax.equals(newMax))) {
            applyShellStates(shellMin, shellMax, false, null);
        }
        shellMin = newMin;
        shellMax = newMax;
        if (newMin != null) {
            applyShellStates(newMin, newMax, true, null);
        }
    }

    /**
     * Schaltet alle Reaktor-Blöcke der Hülle auf geformt/ungeformt und setzt,
     * in welche Richtungen sie an andere Hüllen-Blöcke angrenzen (dort kein Rand).
     */
    private void applyShellStates(BlockPos min, BlockPos max, boolean formed, @Nullable BlockPos skip) {
        if (level == null) return;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!ReactorStructure.isOnShell(pos, min, max) || pos.equals(skip)) continue;
            BlockState state = level.getBlockState(pos);
            if (!ReactorParts.hasProperties(state)) continue; // z.B. Vanilla-Glas

            BlockState updated;
            if (!formed && state.getBlock() instanceof ReactorGlassBlock) {
                // Glas bleibt auch ohne Reaktor mit benachbartem Glas verbunden
                updated = ReactorGlassBlock.withGlassConnections(state, level, pos);
            } else {
                updated = state.setValue(ReactorParts.FORMED, formed);
                for (Direction direction : Direction.values()) {
                    boolean connected = formed && ReactorStructure.isOnShell(pos.relative(direction), min, max);
                    updated = updated.setValue(ReactorParts.connection(direction), connected);
                }
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void unlinkPorts() {
        if (level == null) return;
        for (BlockPos portPos : ports) {
            if (level.getBlockEntity(portPos) instanceof ReactorPortBlockEntity port) {
                port.setController(null);
            }
        }
        ports.clear();
    }

    public boolean isFormed() {
        return structure.formed();
    }

    public boolean hasPort(BlockPos pos) {
        return ports.contains(pos);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        unlinkPorts();
        if (shellMin != null && shellMax != null) {
            applyShellStates(shellMin, shellMax, false, worldPosition);
            shellMin = shellMax = null;
        }
        super.preRemoveSideEffects(pos, state); // Uran und Müll fallen heraus
    }

    // =========================
    // ZUGRIFF FÜR DEN ANSCHLUSS
    // =========================

    public MachineEnergyHandler getEnergyStorage() { return energy; }
    public ResourceHandler<FluidResource> getWaterTank() { return water; }
    public ResourceHandler<ItemResource> getFuelInput() { return fuelInput; }
    public ResourceHandler<ItemResource> getWasteOutput() { return wasteOutput; }

    // =========================
    // GUI-DATEN (Werte werden als short übertragen -> große Zahlen aufteilen)
    // =========================

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_HEAT = 2, DATA_WATER = 3,
            DATA_FUEL = 4, DATA_STATUS = 5, DATA_PROBLEM = 6, DATA_SIZE = 7, DATA_FE_PER_TICK = 8,
            DATA_ACTIVE = 9, DATA_PROBLEM_POS = 10, DATA_COUNT = 11;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_LO -> energy.getAmountAsInt() & 0xFFFF;
                case DATA_ENERGY_HI -> (energy.getAmountAsInt() >>> 16) & 0xFFFF;
                case DATA_HEAT -> heat;
                case DATA_WATER -> water.getAmountAsInt(0);
                case DATA_FUEL -> fuel;
                case DATA_STATUS -> status.ordinal();
                case DATA_PROBLEM -> structure.problem().ordinal();
                case DATA_SIZE -> structure.formed()
                        ? structure.sizeX() | structure.sizeY() << 4 | structure.sizeZ() << 8 : 0;
                case DATA_FE_PER_TICK -> lastFePerTick;
                case DATA_ACTIVE -> active ? 1 : 0;
                case DATA_PROBLEM_POS -> packOffset(structure.where());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-seitig nur lesend; der Client nutzt SimpleContainerData
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Problem-Position relativ zum Controller in 12 Bit (0 = keine). */
    private int packOffset(@Nullable BlockPos where) {
        if (where == null) return 0;
        int dx = where.getX() - worldPosition.getX() + 8;
        int dy = where.getY() - worldPosition.getY() + 8;
        int dz = where.getZ() - worldPosition.getZ() + 8;
        return (dx & 15) | (dy & 15) << 4 | (dz & 15) << 8;
    }

    public void toggleActive() {
        active = !active;
        setChanged();
    }

    // =========================
    // MENÜ
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.jomender.reactor_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ReactorMenu(id, playerInventory, this, data, worldPosition);
    }

    // =========================
    // CONTAINER (2 Slots)
    // =========================

    @Override public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && isFuel(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // =========================
    // SPEICHERN
    // =========================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy.getAmountAsInt());
        output.putInt("fuel", fuel);
        output.putInt("wasteProgress", wasteProgress);
        output.putInt("heat", heat);
        output.putBoolean("active", active);
        output.putBoolean("scram", scram);
        water.serialize(output.child("water"));
        ContainerHelper.saveAllItems(output, items);
        if (isFormed()) {
            output.putLong("boxMin", structure.min().asLong());
            output.putLong("boxMax", structure.max().asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(Math.clamp(input.getIntOr("energy", 0), 0, ENERGY_CAPACITY));
        fuel = Math.max(0, input.getIntOr("fuel", 0));
        wasteProgress = Math.max(0, input.getIntOr("wasteProgress", 0));
        heat = Math.clamp(input.getIntOr("heat", 0), 0, MAX_HEAT);
        active = input.getBooleanOr("active", true);
        scram = input.getBooleanOr("scram", false);
        water.deserialize(input.childOrEmpty("water"));
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        needsCheck = true;

        // Client-Anzeige
        renderWater = water.getAmountAsInt(0);
        long min = input.getLongOr("boxMin", Long.MIN_VALUE);
        long max = input.getLongOr("boxMax", Long.MIN_VALUE);
        if (min != Long.MIN_VALUE && max != Long.MIN_VALUE) {
            renderMin = BlockPos.of(min);
            renderMax = BlockPos.of(max);
        } else {
            renderMin = renderMax = null;
        }
    }

    // =========================
    // CLIENT-SYNC (für die Wasser-Anzeige hinter dem Glas)
    // =========================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public @Nullable BlockPos getRenderMin() { return renderMin; }
    public @Nullable BlockPos getRenderMax() { return renderMax; }

    /** Füllstand 0..1 für die Anzeige. */
    public float getRenderFill() {
        return Math.clamp(renderWater / (float) WATER_CAPACITY, 0F, 1F);
    }

    // =========================
    // HILFSKLASSEN
    // =========================

    /** Wassertank: nimmt nur Wasser an, gibt nach außen nichts ab. */
    private class WaterTank extends FluidStacksResourceHandler {
        WaterTank() {
            super(1, WATER_CAPACITY);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        void setWater(int amount) {
            if (amount <= 0) {
                set(0, FluidResource.EMPTY, 0);
            } else {
                set(0, FluidResource.of(Fluids.WATER), amount);
            }
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    }

    /** Sicht auf genau einen Slot - für die Pipe-Anbindung am Anschluss. */
    private class SlotView implements WorldlyContainer {
        private final int slot;
        private final boolean insert;
        private final boolean extract;

        SlotView(int slot, boolean insert, boolean extract) {
            this.slot = slot;
            this.insert = insert;
            this.extract = extract;
        }

        @Override public int[] getSlotsForFace(Direction side) { return new int[]{slot}; }
        @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) {
            return insert && index == slot && canPlaceItem(index, stack);
        }
        @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
            return extract && index == slot;
        }
        @Override public int getContainerSize() { return ReactorControllerBlockEntity.this.getContainerSize(); }
        @Override public boolean isEmpty() { return ReactorControllerBlockEntity.this.isEmpty(); }
        @Override public ItemStack getItem(int index) { return ReactorControllerBlockEntity.this.getItem(index); }
        @Override public ItemStack removeItem(int index, int amount) { return ReactorControllerBlockEntity.this.removeItem(index, amount); }
        @Override public ItemStack removeItemNoUpdate(int index) { return ReactorControllerBlockEntity.this.removeItemNoUpdate(index); }
        @Override public void setItem(int index, ItemStack stack) { ReactorControllerBlockEntity.this.setItem(index, stack); }
        @Override public boolean canPlaceItem(int index, ItemStack stack) { return ReactorControllerBlockEntity.this.canPlaceItem(index, stack); }
        @Override public void setChanged() { ReactorControllerBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { }
    }
}
