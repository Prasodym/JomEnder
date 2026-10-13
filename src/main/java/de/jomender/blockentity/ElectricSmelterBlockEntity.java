
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.ModRecipes;
import de.jomender.recipe.AlloySmeltingInput;
import de.jomender.recipe.AlloySmeltingRecipe;
import de.jomender.menu.ElectricSmelterMenu;
import de.jomender.sideconfig.SideConfig;
import de.jomender.sideconfig.SideMode;
import de.jomender.upgrade.MachineEnergyHandler;
import de.jomender.upgrade.Upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ElectricSmelterBlockEntity
        extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int MAX_ENERGY = 20000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;

    /** Alle wie viele Ticks fertige Items an Nachbarn geschoben werden. */
    private static final int PUSH_INTERVAL = 4;

    // Inventar:
    // 0-2:  Input
    // 3-10: Output
    // 11-14: Upgrades
    public static final int INPUT_COUNT = 3;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_END = 11;
    public static final int UPGRADE_START = 11;
    public static final int TOTAL_SLOTS = 15;

    private static final int[] INPUT_SLOTS = {0, 1, 2};
    private static final int[] OUTPUT_SLOTS = {3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] NO_SLOTS = {};

    /** Index des Datenslots mit der gepackten Seitenkonfiguration. */
    public static final int DATA_SIDE_CONFIG = 3;
    public static final int DATA_COUNT = 4;

    // FE aufnehmen, nicht abgeben. Die Annahme wächst mit Speed-Upgrades mit.
    private static final int BASE_MAX_RECEIVE = 1000;
    private final MachineEnergyHandler energyStorage =
            new MachineEnergyHandler(MAX_ENERGY, BASE_MAX_RECEIVE, 0, this::setChanged);

    private int progress = 0;
    // Aktuelle Dauer und FE/t mit Upgrades (ohne Upgrades: 200 Ticks, 20 FE/t).
    private int maxProgress = MAX_PROGRESS;
    private int energyPerTick = ENERGY_PER_TICK;
    private int activeInput = -1;
    /** Aktueller Auftrag (Rezept + Slots), damit der Fortschritt bei Änderungen neu startet. */
    private String activeJob = "";
    private int nextInput = 0;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // Seitenkonfiguration. Standard wie bisher: oben Input, unten Output, Rest aus.
    private final SideConfig sideConfig = new SideConfig(defaultSides(), this::onSideConfigChanged);

    // Ein Wrapper pro Weltrichtung; welche Slots er sieht, entscheidet getSlotsForFace.
    @SuppressWarnings("unchecked")
    private final ResourceHandler<ItemResource>[] sideHandlers = new ResourceHandler[6];

    private static SideMode[] defaultSides() {
        SideMode[] d = SideConfig.defaults(SideMode.NONE);
        d[Direction.UP.get3DDataValue()] = SideMode.INPUT;
        d[Direction.DOWN.get3DDataValue()] = SideMode.OUTPUT;
        return d;
    }

    private void onSideConfigChanged() {
        setChanged();
        // Rohre/Kabel müssen ihre zwischengespeicherten Capabilities neu abfragen.
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public SideConfig getSideConfig() {
        return sideConfig;
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getEnergy();
                case 1 -> progress;
                case 2 -> maxProgress;
                case DATA_SIDE_CONFIG -> sideConfig.pack();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.set(
                        Math.clamp(value, 0, MAX_ENERGY)
                );
                case 1 -> progress = Math.max(0, value);
                case 2 -> maxProgress = Math.max(1, value);
                case DATA_SIDE_CONFIG -> sideConfig.unpack(value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
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

    public MachineEnergyHandler getEnergyStorage() {
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
        // Unbekannte Richtung oder Seite aus: kein automatisierter Item-Zugriff.
        if (side == null || sideConfig.get(side) == SideMode.NONE) {
            return null;
        }

        int i = side.get3DDataValue();
        if (sideHandlers[i] == null) {
            sideHandlers[i] = new WorldlyContainerWrapper(this, side);
        }
        return sideHandlers[i];
    }

    // =========================
    // SEITENABHÄNGIGE INVENTARREGELN
    // =========================

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (sideConfig.get(side)) {
            case INPUT -> INPUT_SLOTS;
            case OUTPUT -> OUTPUT_SLOTS;
            case BOTH -> IO_SLOTS;
            case NONE -> NO_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            ItemStack stack,
            Direction side
    ) {
        return side != null
                && sideConfig.get(side).canInput()
                && slot >= 0
                && slot < INPUT_COUNT;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            ItemStack stack,
            Direction side
    ) {
        return side != null
                && sideConfig.get(side).canOutput()
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

    /**
     * Ein Auftrag: entweder ein Legierungsrezept (mehrere Slots) oder ein normales Ofenrezept (ein Slot).
     * key erkennt, ob sich der Auftrag geändert hat (dann beginnt der Fortschritt von vorn).
     */
    private record Job(String key, ItemStack result, int baseTime, int baseEnergy, int[] slots, int[] counts) {}


    private @Nullable Job findJob(ServerLevel level) {
        AlloySmeltingInput input = new AlloySmeltingInput(items.get(0), items.get(1), items.get(2));
        Optional<RecipeHolder<AlloySmeltingRecipe>> alloy =
                level.recipeAccess().getRecipeFor(ModRecipes.ALLOY_SMELTING_TYPE.get(), input, level);
        if (alloy.isPresent()) {
            AlloySmeltingRecipe recipe = alloy.get().value();
            int[] slots = recipe.assign(input);
            if (slots != null) {
                ItemStack result = recipe.resultStack();
                if (!canOutput(result)) {
                    return null; // Ausgabe voll: warten
                }
                int[] counts = recipe.parts().stream().mapToInt(AlloySmeltingRecipe.Part::count).toArray();
                return new Job("alloy:" + alloy.get().id(), result, recipe.time(), recipe.energy(), slots, counts);
            }
        }

        // Normale Ofenrezepte: aktuellen Slot weitermachen, sonst reihum den nächsten suchen
        int slot = activeInput;
        if (slot < 0 || slot >= INPUT_COUNT || !canOutput(getSmeltingResult(level, items.get(slot)))) {
            slot = findNextInput(level);
            activeInput = slot;
        }
        if (slot < 0) {
            return null;
        }
        ItemStack stack = items.get(slot);
        return new Job("smelt:" + slot + ":" + BuiltInRegistries.ITEM.getKey(stack.getItem()),
                getSmeltingResult(level, stack), MAX_PROGRESS, MAX_PROGRESS * ENERGY_PER_TICK,
                new int[]{slot}, new int[]{1});
    }

    public static void serverTick(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            ElectricSmelterBlockEntity smelter
    ) {
        // Fertige Items an Nachbarn auf Output-Seiten abgeben
        // (z.B. zurück in einen AE2 Pattern Provider).
        if (level.getGameTime() % PUSH_INTERVAL == 0) {
            smelter.pushOutputs(level);
        }

        Job job = smelter.findJob(level);
        String key = job == null ? "" : job.key();
        if (!key.equals(smelter.activeJob)) {
            smelter.activeJob = key;
            smelter.progress = 0;
            smelter.setChanged();
        }
        if (job == null) {
            return;
        }

        smelter.applyUpgrades(job.baseTime(), job.baseEnergy());

        if (smelter.getEnergy() < smelter.energyPerTick) {
            return;
        }

        smelter.energyStorage.set(smelter.getEnergy() - smelter.energyPerTick);
        smelter.progress++;

        if (smelter.progress >= smelter.maxProgress) {
            for (int i = 0; i < job.slots().length; i++) {
                smelter.items.get(job.slots()[i]).shrink(job.counts()[i]);
            }
            smelter.insertOutput(job.result());

            smelter.progress = 0;
            smelter.activeJob = "";
            if (job.slots().length == 1) {
                smelter.nextInput = (job.slots()[0] + 1) % INPUT_COUNT;
            }
            smelter.activeInput = -1;
        }

        smelter.setChanged();
    }

    // =========================
    // UPGRADES
    // =========================

    /** Dauer und FE/t aus Rezept-Basiswerten und Upgrade-Slots berechnen. */
    private void applyUpgrades(int baseTime, int baseEnergy) {
        int speed = Upgrades.speedLevel(this, UPGRADE_START, 4);
        int efficiency = Upgrades.efficiencyLevel(this, UPGRADE_START, 4);
        Upgrades.Process process = Upgrades.process(
                baseTime, baseEnergy, speed, efficiency, MAX_ENERGY);
        maxProgress = process.time();
        energyPerTick = process.energyPerTick();
        // Doppelte FE/t annehmen können, damit der Speicher nicht leerläuft.
        energyStorage.setMaxInsert(Math.max(BASE_MAX_RECEIVE, energyPerTick * 2));
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    // =========================
    // AUTO-OUTPUT
    // =========================

    /**
     * Schiebt den Inhalt der Output-Slots in benachbarte Inventare,
     * aber nur über Seiten mit Modus Output oder In & Out.
     */
    private void pushOutputs(ServerLevel level) {
        boolean changed = false;

        for (Direction dir : Direction.values()) {
            if (!sideConfig.get(dir).canOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(dir);
            if (!level.isLoaded(targetPos)) {
                continue;
            }

            ResourceHandler<ItemResource> target = level.getCapability(
                    Capabilities.Item.BLOCK, targetPos, dir.getOpposite());
            if (target == null) {
                continue;
            }

            for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = target.insert(
                            ItemResource.of(stack), stack.getCount(), transaction);
                    if (inserted > 0) {
                        transaction.commit();
                        stack.shrink(inserted);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            setChanged();
        }
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
                data,
                worldPosition
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
        output.putString("activeJob", activeJob);
        output.putInt("nextInput", nextInput);
        sideConfig.save(output, "sideConfig");

        output.putInt("inventoryVersion", 2);
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
                32_000
        );
        activeJob = input.getStringOr("activeJob", "");

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

        sideConfig.load(input, "sideConfig");

        // Existing worlds saved 13 slots: outputs 3..8 and upgrades 9..12.
        // New layout stores 15 slots: outputs 3..10 and upgrades 11..14.
        if (input.getIntOr("inventoryVersion", 1) < 2) {
            NonNullList<ItemStack> old = NonNullList.withSize(13, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, old);
            for (int i = 0; i < 9; i++) items.set(i, old.get(i));
            for (int i = 0; i < 4; i++) items.set(UPGRADE_START + i, old.get(9 + i));
        } else {
            ContainerHelper.loadAllItems(input, items);
        }
    }
}
