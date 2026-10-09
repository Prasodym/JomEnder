
package de.jomender.blockentity;

import de.jomender.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
import java.util.ArrayList;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
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

    public static final int TOTAL_SLOTS = 31;
    private static final int LEGACY_SLOTS = 37;

    private static final int[] OUTPUT_SLOTS = createOutputSlots();
    private static final int[] NO_SLOTS = {};

    private static final int COUNT_SCAN_PER_TICK = 256;

    private int countScanCursor = 0;
    private int foundOres = 0;
    private int minedOres = 0;
    private int countScanProgress = 0;
    private boolean countScanComplete = false;
    private boolean countScanUnloaded = false;


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
                case 6 -> foundOres & 0xFFFF;
                case 7 -> (foundOres >>> 16) & 0xFFFF;
                case 8 -> minedOres & 0xFFFF;
                case 9 -> (minedOres >>> 16) & 0xFFFF;
                case 10 -> countScanProgress;
                case 11 -> (countScanComplete ? 1 : 0)
                        | (countScanUnloaded ? 2 : 0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Die Menüdaten werden vom Server erzeugt.
            // Der Client liest nur die synchronisierten Werte.
        }

        @Override
        public int getCount() {
            return 12;
        }
    };
    private final NonNullList<ItemStack> filters =
            NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);

    private static final TagKey<Block> ORE_TAG = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            Identifier.fromNamespaceAndPath("c", "ores")
    );

    public ItemStack getFilter(int filterIndex) {
        if (filterIndex < 0 || filterIndex >= FILTER_SLOTS) return ItemStack.EMPTY;
        return filters.get(filterIndex);
    }

    public void setFilter(int filterIndex, ItemStack template) {
        if (filterIndex < 0 || filterIndex >= FILTER_SLOTS) return;
        if (template.isEmpty()) {
            filters.set(filterIndex, ItemStack.EMPTY);
            setChanged();
            return;
        }
        if (!(template.getItem() instanceof BlockItem blockItem)) return;
        if (!blockItem.getBlock().defaultBlockState().is(ORE_TAG)) return;
        filters.set(filterIndex, template.copyWithCount(1));
        setChanged();
    }

    public boolean matchesAnyFilter(ItemStack candidate) {
        if (candidate.isEmpty()) return false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = getFilter(i);
            if (!filter.isEmpty() && filter.is(candidate.getItem())) return true;
        }
        return false;
    }

    // Ghost-Muster werden bewusst ausschließlich als Item-IDs persistiert.
    private String filterId(int index) {
        ItemStack filter = filters.get(index);
        return filter.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(filter.getItem()).toString();
    }

    private void restoreFilter(int index, String id) {
        filters.set(index, ItemStack.EMPTY);
        if (id == null || id.isBlank()) return;
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item != null) {
                // Validation auch beim Laden, nicht nur bei GUI-Klicks.
                setFilter(index, new ItemStack(item));
            }
        } catch (IllegalArgumentException ignored) {
            // Alte / ungültige Einträge nicht übernehmen.
        }
    }

    private void restoreLegacyInventory(NonNullList<ItemStack> oldItems) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            items.set(i, oldItems.get(i));
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!oldItems.get(FILTER_START + i).isEmpty()) {
                setFilter(i, oldItems.get(FILTER_START + i));
            }
        }
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

    private void countOreTick(ServerLevel level) {
        boolean hasFilter = false;

        for (ItemStack filter : filters) {
            if (!filter.isEmpty()) {
                hasFilter = true;
                break;
            }
        }

        if (!hasFilter) {
            countScanCursor = 0;
            foundOres = 0;
            countScanProgress = 0;
            countScanComplete = false;
            countScanUnloaded = false;
            return;
        }

        if (countScanComplete) {
            return;
        }

        int bottom = Math.max(minY, level.getMinY());
        int top = Math.min(maxY, level.getMaxY() - 1);

        if (bottom > top) {
            countScanProgress = 100;
            countScanComplete = true;
            return;
        }

        int diameter = radius * 2 + 1;
        int height = top - bottom + 1;
        int total = diameter * diameter * height;

        for (int i = 0;
             i < COUNT_SCAN_PER_TICK && countScanCursor < total;
             i++) {

            int cursor = countScanCursor++;

            int x = cursor % diameter - radius;
            int z = (cursor / diameter) % diameter - radius;
            int y = cursor / (diameter * diameter) + bottom;

            if (Math.abs(x) <= 1
                    && Math.abs(z) <= 1
                    && y >= worldPosition.getY()
                    && y <= worldPosition.getY() + 2) {
                continue;
            }

            BlockPos target = worldPosition.offset(
                    x,
                    y - worldPosition.getY(),
                    z
            );

            if (!level.getChunkSource().hasChunk(
                    target.getX() >> 4,
                    target.getZ() >> 4
            )) {
                countScanUnloaded = true;
                continue;
            }

            BlockState found = level.getBlockState(target);

            if (found.is(ORE_TAG)
                    && !found.hasBlockEntity()
                    && matchesAnyFilter(
                    new ItemStack(found.getBlock())
            )) {
                foundOres++;
            }
        }

        countScanProgress = (int) (
                countScanCursor * 100L / total
        );

        if (countScanCursor >= total) {
            countScanProgress = 100;
            countScanComplete = true;
        }
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

    // Maximal ein Erz je 10 Ticks; 256 Positionen pro Suchlauf.
    private static final int MINING_COST = 1_000;
    private static final int SCAN_PER_STEP = 256;
    private int scanCursor = 0;
    private int miningTicks = 0;

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
    // MINING-ENGINE (serverseitig)
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GermaniumMinerBlockEntity miner
    ) {
        if (level instanceof ServerLevel serverLevel) {
            miner.countOreTick(serverLevel);
            miner.mineTick(serverLevel);
        }
    }

    private void mineTick(ServerLevel level) {
        if (!running || getEnergy() < MINING_COST) return;
        if (++miningTicks < 10) return;
        miningTicks = 0;

        boolean anyFilter = false;
        for (ItemStack filter : filters) {
            if (!filter.isEmpty()) { anyFilter = true; break; }
        }
        if (!anyFilter) return;

        int bottom = Math.max(minY, level.getMinY());
        int top = Math.min(maxY, level.getMaxY() - 1);
        if (bottom > top) return;

        int diameter = radius * 2 + 1;
        int height = top - bottom + 1;
        int total = diameter * diameter * height;
        if (total <= 0) return;
        scanCursor = Math.floorMod(scanCursor, total);

        for (int attempt = 0; attempt < SCAN_PER_STEP; attempt++) {
            int cursor = scanCursor;
            scanCursor = (scanCursor + 1) % total;

            int x = cursor % diameter - radius;
            int z = (cursor / diameter) % diameter - radius;
            int y = cursor / (diameter * diameter) + bottom;
            BlockPos target = worldPosition.offset(x, y - worldPosition.getY(), z);

            // Das Maschinengehäuse nie anfassen.
            if (Math.abs(x) <= 1 && Math.abs(z) <= 1
                    && Math.abs(y - worldPosition.getY()) <= 2) continue;

            // Keine Chunks nur wegen des Miners nachladen.
            if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)) continue;

            BlockState found = level.getBlockState(target);
            if (!found.is(ORE_TAG) || found.hasBlockEntity()) continue;
            if (!matchesAnyFilter(new ItemStack(found.getBlock()))) continue;
            if (found.getDestroySpeed(level, target) < 0) continue;

            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            if (silkTouch) {
                tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH), 1);
            }

            List<ItemStack> drops = found.getDrops(new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, target.getCenter())
                    .withParameter(LootContextParams.BLOCK_STATE, found)
                    .withParameter(LootContextParams.TOOL, tool));
            if (drops.isEmpty()) continue;

            // Probeeinlagerung in einer Kopie: nichts zerstören, wenn es nicht passt.
            List<ItemStack> planned = new ArrayList<>(STORAGE_SLOTS);
            for (int slot = 0; slot < STORAGE_SLOTS; slot++) planned.add(items.get(slot).copy());
            boolean fits = true;
            for (ItemStack drop : drops) {
                if (!insertInto(planned, drop.copy())) { fits = false; break; }
            }
            if (!fits) {
                // Scanner an diesem Erz anhalten, statt es zu überspringen.
                scanCursor = cursor;
                return;
            }
            if (getEnergy() < MINING_COST) return;

            // Erst nach erfolgreichem Blockaustausch werden Drops + FE übernommen.
            if (!level.setBlock(target, Blocks.DIRT.defaultBlockState(), 3)) return;
            for (int slot = 0; slot < STORAGE_SLOTS; slot++) items.set(slot, planned.get(slot));
            energyStorage.set(getEnergy() - MINING_COST);

            minedOres++;

            // Ein bereits gezähltes Erz aus dem Zähler entfernen.
            if (countScanComplete && foundOres > 0) {
                foundOres--;
            }

            setChanged();
            return;
        }
        setChanged(); // Suchfortschritt für Welt-Speicherung
    }

    private static boolean insertInto(List<ItemStack> storage, ItemStack incoming) {
        if (incoming.isEmpty()) return true;
        for (int pass = 0; pass < 2 && !incoming.isEmpty(); pass++) {
            for (int slot = 0; slot < storage.size() && !incoming.isEmpty(); slot++) {
                ItemStack stored = storage.get(slot);
                if (pass == 0 && stored.isEmpty()) continue;
                if (pass == 1 && !stored.isEmpty()) continue;
                if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, incoming)) continue;
                int maximum = incoming.getMaxStackSize();
                if (!stored.isEmpty()) maximum = Math.min(maximum, stored.getMaxStackSize());
                int available = maximum - stored.getCount();
                if (available <= 0) continue;
                int amount = Math.min(available, incoming.getCount());
                if (stored.isEmpty()) storage.set(slot, incoming.copyWithCount(amount));
                else stored.grow(amount);
                incoming.shrink(amount);
            }
        }
        return incoming.isEmpty();
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

    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state
    ) {
        // Absichtlich leer!
        //
        // Minecraft darf die Inventarinhalte beim
        // Abbauen NICHT separat auf den Boden werfen.
        //
        // Die Loot Table überträgt sie stattdessen
        // über minecraft:container in das Miner-Item.
        //
        // Ghost-Filter bleiben als Einstellungen
        // im Custom-Data-Component gespeichert.

        // WICHTIG: Hier NICHT super.preRemoveSideEffects(...)
        // aufrufen!
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
        output.putInt("scanCursor", scanCursor);

        output.putBoolean("running", running);
        output.putBoolean("silkTouch", silkTouch);
        output.putBoolean("autoPush", autoPush);
        output.putInt("minedOres", minedOres);

        ContainerHelper.saveAllItems(output, items);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            output.putString("ghost_filter_" + i, filterId(i));
        }
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
        scanCursor = Math.max(0, input.getIntOr("scanCursor", 0));

        running = input.getBooleanOr("running", false);
        silkTouch = input.getBooleanOr("silkTouch", false);
        autoPush = input.getBooleanOr("autoPush", false);
        minedOres = Math.max(0, input.getIntOr("minedOres", 0));

        // Migration: ältere Versionen hatten Filter als echte Slots 31–36.
        NonNullList<ItemStack> oldItems =
                NonNullList.withSize(LEGACY_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, oldItems);
        restoreLegacyInventory(oldItems);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            String id = input.getStringOr("ghost_filter_" + i, "");
            if (!id.isEmpty()) restoreFilter(i, id);
        }
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
        data.putInt("scanCursor", scanCursor);

        data.putBoolean("running", running);
        data.putBoolean("silkTouch", silkTouch);
        data.putBoolean("autoPush", autoPush);
        data.putInt("minedOres", minedOres);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            data.putString("ghost_filter_" + i, filterId(i));
        }

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
            NonNullList<ItemStack> oldItems =
                    NonNullList.withSize(LEGACY_SLOTS, ItemStack.EMPTY);
            contents.copyInto(oldItems);
            restoreLegacyInventory(oldItems);
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
            scanCursor = Math.max(0, data.getIntOr("scanCursor", 0));

            running = data.getBooleanOr("running", false);
            silkTouch = data.getBooleanOr("silkTouch", false);
            autoPush = data.getBooleanOr("autoPush", false);
            minedOres = Math.max(0, data.getIntOr("minedOres", 0));
            for (int i = 0; i < FILTER_SLOTS; i++) {
                String id = data.getStringOr("ghost_filter_" + i, "");
                if (!id.isEmpty()) restoreFilter(i, id);
            }
        }

        setChanged();
    }
}
