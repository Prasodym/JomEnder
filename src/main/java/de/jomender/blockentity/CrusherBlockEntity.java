package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.block.CrusherBlock;
import de.jomender.crusher.CrusherRecipes;
import de.jomender.menu.CrusherMenu;
import de.jomender.recipe.CrushingRecipe;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Crusher: zermahlt Items zu Staub. Aufbau wie der Electric Smelter
 * (3 Eingänge, 8 Ausgänge, 4 Upgrade-Slots, Seitenkonfiguration, FE-Eingang).
 * Rezepte: siehe {@link CrusherRecipes}.
 */
public class CrusherBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int MAX_ENERGY = 20000;

    /** Alle wie viele Ticks fertige Items an Nachbarn geschoben werden. */
    private static final int PUSH_INTERVAL = 4;

    // Inventar: 0-2 Input, 3-10 Output, 11-14 Upgrades
    public static final int INPUT_COUNT = 3;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_END = 11;
    public static final int UPGRADE_START = 11;
    public static final int TOTAL_SLOTS = 15;

    private static final int[] INPUT_SLOTS = {0,1,2};
    private static final int[] OUTPUT_SLOTS = {3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] NO_SLOTS = {};

    /** Index des Datenslots mit der gepackten Seitenkonfiguration. */
    public static final int DATA_SIDE_CONFIG = 3;
    public static final int DATA_COUNT = 4;

    private static final int BASE_MAX_RECEIVE = 1000;
    private final MachineEnergyHandler energyStorage =
            new MachineEnergyHandler(MAX_ENERGY, BASE_MAX_RECEIVE, 0, this::setChanged);

    private int progress = 0;
    private int maxProgress = CrushingRecipe.DEFAULT_TIME;
    private int energyPerTick = CrushingRecipe.DEFAULT_ENERGY / CrushingRecipe.DEFAULT_TIME;
    private int activeInput = -1;
    /** Aktueller Auftrag (Slot + Item), damit der Fortschritt bei Änderungen neu startet. */
    private String activeJob = "";
    private int nextInput = 0;

    /** Nur Client: letzte Stellung der Walzen (damit sie beim Anhalten nicht zurückspringen). */
    public float clientRollerAngle;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // Standard: oben Input, unten Output, Rest aus.
    private final SideConfig sideConfig = new SideConfig(defaultSides(), this::onSideConfigChanged);

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
                case 0 -> energyStorage.set(Math.clamp(value, 0, MAX_ENERGY));
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

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    // =========================
    // FE / ITEMS
    // =========================

    public MachineEnergyHandler getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergy() {
        return energyStorage.getAmountAsInt();
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        if (side == null || sideConfig.get(side) == SideMode.NONE) {
            return null;
        }
        int i = side.get3DDataValue();
        if (sideHandlers[i] == null) {
            sideHandlers[i] = new WorldlyContainerWrapper(this, side);
        }
        return sideHandlers[i];
    }

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
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && sideConfig.get(side).canInput() && slot >= 0 && slot < INPUT_COUNT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != null && sideConfig.get(side).canOutput() && slot >= OUTPUT_START && slot < OUTPUT_END;
    }

    // =========================
    // AUSGABE
    // =========================

    /** Passen alle Ergebnisse (mit voller Anzahl) gleichzeitig in die Output-Slots? */
    private boolean canOutputAll(List<ItemStack> results) {
        if (results.isEmpty()) return false;
        List<ItemStack> sim = new ArrayList<>(OUTPUT_END - OUTPUT_START);
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            sim.add(items.get(slot).copy());
        }
        for (ItemStack result : results) {
            if (!insertInto(sim, result.copy())) return false;
        }
        return true;
    }

    /** Fügt den Stapel in die Liste ein (erst auffüllen, dann freie Slots). false, wenn etwas übrig bleibt. */
    private boolean insertInto(List<ItemStack> slots, ItemStack stack) {
        for (ItemStack current : slots) {
            if (stack.isEmpty()) return true;
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                int limit = Math.min(current.getMaxStackSize(), getMaxStackSize(current));
                int move = Math.min(stack.getCount(), limit - current.getCount());
                if (move > 0) {
                    current.grow(move);
                    stack.shrink(move);
                }
            }
        }
        for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) {
                int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getMaxStackSize(stack)));
                slots.set(i, stack.split(move));
            }
        }
        return stack.isEmpty();
    }

    private void insertOutputs(List<ItemStack> results) {
        List<ItemStack> outputs = items.subList(OUTPUT_START, OUTPUT_END);
        for (ItemStack result : results) {
            insertInto(outputs, result.copy());
        }
    }

    // =========================
    // ZERKLEINERN
    // =========================

    private record Job(String key, int slot, CrushingRecipe recipe) {}

    private @Nullable CrushingRecipe recipeFor(ServerLevel level, int slot) {
        ItemStack stack = items.get(slot);
        CrushingRecipe recipe = CrusherRecipes.find(level, stack);
        if (recipe == null || stack.getCount() < recipe.inputCount()) return null;
        return canOutputAll(recipe.maxResults()) ? recipe : null;
    }

    private @Nullable Job findJob(ServerLevel level) {
        int slot = activeInput;
        CrushingRecipe recipe = slot >= 0 && slot < INPUT_COUNT ? recipeFor(level, slot) : null;
        if (recipe == null) {
            slot = -1;
            for (int offset = 0; offset < INPUT_COUNT; offset++) {
                int candidate = (nextInput + offset) % INPUT_COUNT;
                recipe = recipeFor(level, candidate);
                if (recipe != null) {
                    slot = candidate;
                    break;
                }
            }
            activeInput = slot;
        }
        if (slot < 0 || recipe == null) return null;
        String key = slot + ":" + BuiltInRegistries.ITEM.getKey(items.get(slot).getItem());
        return new Job(key, slot, recipe);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CrusherBlockEntity crusher) {
        if (level.getGameTime() % PUSH_INTERVAL == 0) {
            crusher.pushOutputs(level);
        }

        Job job = crusher.findJob(level);
        String key = job == null ? "" : job.key();
        if (!key.equals(crusher.activeJob)) {
            crusher.activeJob = key;
            crusher.progress = 0;
            crusher.setChanged();
        }
        if (job == null) {
            crusher.setActive(level, pos, state, false);
            return;
        }

        crusher.applyUpgrades(job.recipe().time(), job.recipe().energy());

        if (crusher.getEnergy() < crusher.energyPerTick) {
            crusher.setActive(level, pos, state, false);
            return;
        }

        crusher.energyStorage.set(crusher.getEnergy() - crusher.energyPerTick);
        crusher.progress++;
        crusher.setActive(level, pos, state, true);

        if (crusher.progress >= crusher.maxProgress) {
            crusher.items.get(job.slot()).shrink(job.recipe().inputCount());
            crusher.insertOutputs(job.recipe().rollResults(level.getRandom()));

            crusher.progress = 0;
            crusher.activeJob = "";
            crusher.nextInput = (job.slot() + 1) % INPUT_COUNT;
            crusher.activeInput = -1;
        }

        crusher.setChanged();
    }

    /** Walzen drehen sich nur, solange gearbeitet wird (Blockstate "active"). */
    private void setActive(ServerLevel level, BlockPos pos, BlockState state, boolean active) {
        if (state.hasProperty(CrusherBlock.ACTIVE) && state.getValue(CrusherBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(CrusherBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    // =========================
    // UPGRADES
    // =========================

    private void applyUpgrades(int baseTime, int baseEnergy) {
        int speed = Upgrades.speedLevel(this, UPGRADE_START, 4);
        int efficiency = Upgrades.efficiencyLevel(this, UPGRADE_START, 4);
        Upgrades.Process process = Upgrades.process(baseTime, baseEnergy, speed, efficiency, MAX_ENERGY);
        maxProgress = process.time();
        energyPerTick = process.energyPerTick();
        energyStorage.setMaxInsert(Math.max(BASE_MAX_RECEIVE, energyPerTick * 2));
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    // =========================
    // AUTO-OUTPUT
    // =========================

    private void pushOutputs(ServerLevel level) {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (!sideConfig.get(dir).canOutput()) continue;
            BlockPos targetPos = worldPosition.relative(dir);
            if (!level.isLoaded(targetPos)) continue;
            ResourceHandler<ItemResource> target =
                    level.getCapability(Capabilities.Item.BLOCK, targetPos, dir.getOpposite());
            if (target == null) continue;

            for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = target.insert(ItemResource.of(stack), stack.getCount(), transaction);
                    if (inserted > 0) {
                        transaction.commit();
                        stack.shrink(inserted);
                        changed = true;
                    }
                }
            }
        }
        if (changed) setChanged();
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
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
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
        boolean activeInputChanged = slot == activeInput && !ItemStack.isSameItemSameComponents(previous, stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
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
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64;
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
        return Component.translatable("block.jomender.crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CrusherMenu(id, playerInventory, this, data, worldPosition);
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
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.set(Math.clamp(input.getIntOr("energy", 0), 0, MAX_ENERGY));
        progress = Math.clamp(input.getIntOr("progress", 0), 0, 32_000);
        activeJob = input.getStringOr("activeJob", "");
        activeInput = Math.clamp(input.getIntOr("activeInput", -1), -1, INPUT_COUNT - 1);
        nextInput = Math.clamp(input.getIntOr("nextInput", 0), 0, INPUT_COUNT - 1);
        sideConfig.load(input, "sideConfig");
        ContainerHelper.loadAllItems(input, items);
    }
}
