package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.ModParticles;
import de.jomender.ModRecipes;
import de.jomender.menu.ArcFusionChamberMenu;
import de.jomender.recipe.ArcFusionInput;
import de.jomender.recipe.ArcFusionRecipe;
import de.jomender.sideconfig.SideConfig;
import de.jomender.sideconfig.SideMode;
import de.jomender.upgrade.MachineEnergyHandler;
import de.jomender.upgrade.Upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Optional;

/**
 * Arc Fusion Chamber: verschmilzt zwei Items nach JSON-Rezept (Typ jomender:arc_fusion).
 * Energie nur von unten, Items nur über die (konfigurierbare) Oberseite.
 */
public class ArcFusionChamberBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    // =========================
    // KONSTANTEN
    // =========================

    // Hinweis: GUI-Datenslots werden als Short übertragen, daher < 32767.
    public static final int MAX_ENERGY = 30_000;
    public static final int MAX_RECEIVE = 1_000;

    // Slots: 0-1 Input, 2-3 Output, 4-7 Upgrades
    public static final int INPUT_START = 0;
    public static final int INPUT_COUNT = 2;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_COUNT = 2;
    public static final int UPGRADE_START = 4;
    public static final int UPGRADE_COUNT = 4;
    public static final int TOTAL_SLOTS = 8;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_MAX_PROGRESS = 2;
    public static final int DATA_SIDE_CONFIG = 3;
    public static final int DATA_ENERGY_PER_TICK = 4;
    public static final int DATA_COUNT = 5;

    private static final int PUSH_INTERVAL = 4;
    private static final int SYNC_INTERVAL = 20;

    private static final int[] INPUT_SLOTS = {0, 1};
    private static final int[] OUTPUT_SLOTS = {2, 3};
    private static final int[] IO_SLOTS = {0, 1, 2, 3};
    private static final int[] NO_SLOTS = {};

    /** Mitte der Glaskammer relativ zum Block (Modell: x/z 3-13, y 5-13 Pixel). */
    public static final double CHAMBER_CENTER_Y = 9.0 / 16.0;

    // =========================
    // ZUSTAND
    // =========================

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // FE nur aufnehmen, nicht abgeben.
    // Annahme pro Übertragung wächst mit Speed-Upgrades mit.
    private final MachineEnergyHandler energyStorage =
            new MachineEnergyHandler(MAX_ENERGY, MAX_RECEIVE, 0, this::setChanged);

    private int progress = 0;
    private int maxProgress = ArcFusionRecipe.DEFAULT_TIME;
    private int energyPerTick = 0;
    /** true, solange gerade verschmolzen wird (für Animation und Partikel). */
    private boolean active = false;

    // Nur Server: Rezept-Cache
    private boolean recipeDirty = true;
    private RecipeHolder<ArcFusionRecipe> cachedRecipe;
    // Nur Server: Rezept, an dem gerade gearbeitet wird (Wechsel = Fortschritt zurücksetzen)
    private Object runningRecipeId;
    // Nur Server: Client muss neue Items/Status bekommen
    private boolean syncDirty = false;
    // Nur Server: letzter bekannter Stand der Slots 0-3, um Änderungen zu erkennen
    // (auch wenn das GUI Stapel direkt verändert, z.B. beim Stapeln).
    private final ItemStack[] lastSeen = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    // Nur Client: Fortschritt für flüssige Animation zwischen den Syncs
    private float clientProgress = 0;

    // Seitenkonfiguration: nur die Oberseite ist einstellbar (Standard: In & Out).
    private final SideConfig sideConfig = new SideConfig(defaultSides(), this::onSideConfigChanged);

    @SuppressWarnings("unchecked")
    private final ResourceHandler<ItemResource>[] sideHandlers = new ResourceHandler[6];

    private static SideMode[] defaultSides() {
        SideMode[] d = SideConfig.defaults(SideMode.NONE);
        d[Direction.UP.get3DDataValue()] = SideMode.BOTH;
        return d;
    }

    public static boolean isConfigurableSide(Direction side) {
        return side == Direction.UP;
    }

    private void onSideConfigChanged() {
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> getEnergy();
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> maxProgress;
                case DATA_SIDE_CONFIG -> sideConfig.pack();
                case DATA_ENERGY_PER_TICK -> energyPerTick;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_ENERGY -> energyStorage.set(Math.clamp(value, 0, MAX_ENERGY));
                case DATA_PROGRESS -> progress = Math.max(0, value);
                case DATA_MAX_PROGRESS -> maxProgress = Math.max(1, value);
                // Nur die Oberseite darf sich ändern; andere Seiten bleiben gesperrt.
                case DATA_SIDE_CONFIG -> sideConfig.unpack(onlyTop(value));
                case DATA_ENERGY_PER_TICK -> energyPerTick = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Übernimmt aus einem gepackten Wert nur die Oberseite, alle anderen Seiten bleiben aus. */
    private static int onlyTop(int packed) {
        SideMode top = SideConfig.read(packed, Direction.UP);
        SideMode[] modes = SideConfig.defaults(SideMode.NONE);
        modes[Direction.UP.get3DDataValue()] = top;
        SideConfig tmp = new SideConfig(modes, () -> {});
        return tmp.pack();
    }

    public ArcFusionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FUSION_CHAMBER.get(), pos, state);
    }

    // =========================
    // CAPABILITIES
    // =========================

    public MachineEnergyHandler getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergy() {
        return energyStorage.getAmountAsInt();
    }

    /** Strom nur von unten (null = interner Zugriff). */
    public EnergyHandler getEnergyHandler(Direction side) {
        return side == null || side == Direction.DOWN ? energyStorage : null;
    }

    public ResourceHandler<ItemResource> getItemHandler(Direction side) {
        if (side == null || sideConfig.get(side) == SideMode.NONE) {
            return null;
        }
        int i = side.get3DDataValue();
        if (sideHandlers[i] == null) {
            sideHandlers[i] = new WorldlyContainerWrapper(this, side);
        }
        return sideHandlers[i];
    }

    public SideConfig getSideConfig() {
        return sideConfig;
    }

    // =========================
    // SEITENREGELN FÜR ROHRE
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
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == null || !sideConfig.get(side).canInput() || slot < 0 || slot >= INPUT_COUNT) {
            return false;
        }
        // Verteilt automatisch: ein Item-Typ belegt nur einen Input-Slot,
        // damit Rohre nicht beide Slots mit demselben Item füllen.
        ItemStack current = items.get(slot);
        if (!current.isEmpty()) {
            return ItemStack.isSameItemSameComponents(current, stack);
        }
        ItemStack other = items.get(INPUT_COUNT - 1 - slot);
        return other.isEmpty() || !ItemStack.isSameItemSameComponents(other, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != null
                && sideConfig.get(side).canOutput()
                && slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_COUNT;
    }

    // =========================
    // REZEPTE
    // =========================

    private ArcFusionInput currentInput() {
        return new ArcFusionInput(items.get(0), items.get(1));
    }

    private RecipeHolder<ArcFusionRecipe> findRecipe(ServerLevel level) {
        if (recipeDirty) {
            recipeDirty = false;
            ArcFusionInput input = currentInput();
            if (input.isEmpty()) {
                cachedRecipe = null;
            } else {
                Optional<RecipeHolder<ArcFusionRecipe>> found = level.recipeAccess()
                        .getRecipeFor(ModRecipes.ARC_FUSION_TYPE.get(), input, level);
                cachedRecipe = found.orElse(null);
            }
        }
        return cachedRecipe;
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        int remaining = result.getCount();
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_COUNT; slot++) {
            ItemStack current = items.get(slot);
            int limit = Math.min(result.getMaxStackSize(), getMaxStackSize(result));
            if (current.isEmpty()) {
                remaining -= limit;
            } else if (ItemStack.isSameItemSameComponents(current, result)) {
                remaining -= limit - current.getCount();
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private void insertOutput(ItemStack result) {
        ItemStack rest = result.copy();
        // Erst passende Stapel auffüllen, dann freie Slots.
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_COUNT && !rest.isEmpty(); slot++) {
                ItemStack current = items.get(slot);
                int limit = Math.min(rest.getMaxStackSize(), getMaxStackSize(rest));
                if (pass == 0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, rest)) {
                    int move = Math.min(rest.getCount(), limit - current.getCount());
                    if (move > 0) {
                        current.grow(move);
                        rest.shrink(move);
                    }
                } else if (pass == 1 && current.isEmpty()) {
                    int move = Math.min(rest.getCount(), limit);
                    items.set(slot, rest.split(move));
                }
            }
        }
    }

    // =========================
    // SERVER-TICK
    // =========================

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ArcFusionChamberBlockEntity be) {
        be.detectSlotChanges();

        if (level.getGameTime() % PUSH_INTERVAL == 0) {
            be.pushOutputs(level);
        }

        RecipeHolder<ArcFusionRecipe> holder = be.findRecipe(level);
        ArcFusionRecipe recipe = holder == null ? null : holder.value();

        Object recipeId = holder == null ? null : holder.id();
        if (!java.util.Objects.equals(recipeId, be.runningRecipeId)) {
            // Nach dem Laden der Welt (noch kein Rezept bekannt) den gespeicherten Fortschritt behalten.
            if (be.runningRecipeId != null || recipeId == null) {
                be.progress = 0;
            }
            be.runningRecipeId = recipeId;
        }

        if (recipe == null || !be.canOutput(recipe.resultStack())) {
            if (be.progress != 0 || be.active) {
                be.progress = 0;
                be.energyPerTick = 0;
                be.setActive(false);
                be.setChanged();
            }
        } else {
            // Rezeptwerte aus der JSON mit Upgrades verrechnen
            int speed = Upgrades.speedLevel(be, UPGRADE_START, UPGRADE_COUNT);
            int efficiency = Upgrades.efficiencyLevel(be, UPGRADE_START, UPGRADE_COUNT);
            Upgrades.Process process = Upgrades.process(recipe.time(), recipe.energy(), speed, efficiency, MAX_ENERGY);
            if (be.maxProgress != process.time()) {
                be.maxProgress = process.time();
                be.syncDirty = true; // Animation braucht die neue Dauer
            }
            be.energyPerTick = process.energyPerTick();
            be.energyStorage.setMaxInsert(Math.max(MAX_RECEIVE, be.energyPerTick * 2));

            if (be.getEnergy() >= be.energyPerTick) {
                be.energyStorage.set(be.getEnergy() - be.energyPerTick);
                be.progress++;
                be.setActive(true);

                if (be.progress >= be.maxProgress) {
                    be.finishCrafting(level, pos, recipe);
                }
                be.setChanged();
            } else {
                // Kein Strom: Fortschritt bleibt stehen.
                be.setActive(false);
            }
        }

        // Fortschritt regelmäßig an den Client, damit die Animation synchron bleibt.
        if (be.active && level.getGameTime() % SYNC_INTERVAL == 0) {
            be.syncDirty = true;
        }
        if (be.syncDirty) {
            be.syncDirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void detectSlotChanges() {
        for (int slot = 0; slot < lastSeen.length; slot++) {
            ItemStack now = items.get(slot);
            if (!ItemStack.matches(lastSeen[slot], now)) {
                lastSeen[slot] = now.copy();
                syncDirty = true;
                if (slot < INPUT_COUNT) {
                    recipeDirty = true;
                }
            }
        }
    }

    private void setActive(boolean value) {
        if (active != value) {
            active = value;
            syncDirty = true;
        }
    }

    private void finishCrafting(ServerLevel level, BlockPos pos, ArcFusionRecipe recipe) {
        ArcFusionInput input = currentInput();
        int useFirst = recipe.countForFirstSlot(input);
        int useSecond = recipe.countForSecondSlot(input);
        ItemStack result = recipe.assemble(input);

        items.get(0).shrink(useFirst);
        items.get(1).shrink(useSecond);
        insertOutput(result);

        progress = 0;
        recipeDirty = true;
        syncDirty = true;

        playFinishEffects(level, pos);
    }

    /** Der große Moment: Lichtblitz, Funken und Klang. */
    private static void playFinishEffects(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + CHAMBER_CENTER_Y;
        double cz = pos.getZ() + 0.5;

        level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 24, 0.12, 0.12, 0.12, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy, cz, 40, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 20, 0.2, 0.2, 0.2, 0.25);
        level.sendParticles(ModParticles.ENDARIUM_LIGHTNING.get(), cx, cy, cz, 12, 0.22, 0.2, 0.22, 0.0);

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.7F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    // =========================
    // CLIENT-TICK: ANIMATION UND PARTIKEL
    // =========================

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArcFusionChamberBlockEntity be) {
        if (!be.active) {
            return;
        }
        be.clientProgress = Math.min(be.maxProgress, be.clientProgress + 1);

        RandomSource r = level.getRandom();
        float p = be.getProgressFraction(0);
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + CHAMBER_CENTER_Y;
        double cz = pos.getZ() + 0.5;

        // Energie strömt in die Mitte: Partikel am Kammerrand mit Bewegung zur Mitte.
        int streams = 1 + (int) (p * 3);
        for (int i = 0; i < streams; i++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double radius = 0.26;
            double sx = cx + Math.cos(angle) * radius;
            double sz = cz + Math.sin(angle) * radius;
            double sy = cy + (r.nextDouble() - 0.5) * 0.35;
            double speed = 0.02 + p * 0.02;
            level.addParticle(ParticleTypes.REVERSE_PORTAL, sx, sy, sz,
                    (cx - sx) * speed * 2, (cy - sy) * speed * 2, (cz - sz) * speed * 2);
        }

        // Elektrische Bögen zwischen den kreisenden Items
        if (r.nextFloat() < 0.35F + p * 0.5F) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    cx + (r.nextDouble() - 0.5) * 0.3, cy + (r.nextDouble() - 0.5) * 0.25, cz + (r.nextDouble() - 0.5) * 0.3,
                    0, 0, 0);
        }
        if (level.getGameTime() % 6 == 0 || (p > 0.8F && r.nextFloat() < 0.4F)) {
            level.addParticle(ModParticles.ENDARIUM_LIGHTNING.get(),
                    cx + (r.nextDouble() - 0.5) * 0.25, cy + (r.nextDouble() - 0.5) * 0.2, cz + (r.nextDouble() - 0.5) * 0.25,
                    0, 0, 0);
        }
        if (r.nextFloat() < 0.08F) {
            level.addParticle(ParticleTypes.END_ROD, cx, cy - 0.1, cz,
                    (r.nextDouble() - 0.5) * 0.01, 0.01, (r.nextDouble() - 0.5) * 0.01);
        }

        // Leises Summen
        if (level.getGameTime() % 40 == 0) {
            level.playLocalSound(pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.35F, 1.5F + p * 0.4F, false);
        }
    }

    // =========================
    // FÜR DEN RENDERER
    // =========================

    public boolean isActive() {
        return active;
    }

    /** Fortschritt 0..1 inklusive Zwischen-Tick für flüssige Bewegung. */
    public float getProgressFraction(float partialTick) {
        if (maxProgress <= 0) return 0;
        float value = clientProgress + (active ? partialTick : 0);
        return Math.clamp(value / maxProgress, 0F, 1F);
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

            for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_COUNT; slot++) {
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
        if (changed) {
            syncDirty = true;
            setChanged();
        }
    }

    // =========================
    // INVENTAR
    // =========================

    private void onSlotChanged(int slot) {
        if (slot < INPUT_COUNT) {
            recipeDirty = true;
        }
        if (slot < UPGRADE_START) {
            syncDirty = true; // Items in der Glaskammer anzeigen
        }
        setChanged();
    }

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) onSlotChanged(slot);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        onSlotChanged(slot);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        onSlotChanged(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= INPUT_START && slot < INPUT_START + INPUT_COUNT;
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
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        progress = 0;
        recipeDirty = true;
        syncDirty = true;
        setChanged();
    }

    // =========================
    // GUI
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.jomender.arc_fusion_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ArcFusionChamberMenu(id, playerInventory, this, data, worldPosition);
    }

    // =========================
    // CLIENT-SYNC
    // =========================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // =========================
    // SPEICHERN
    // =========================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", getEnergy());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("energyPerTick", energyPerTick);
        output.putBoolean("active", active);
        sideConfig.save(output, "sideConfig");
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.set(Math.clamp(input.getIntOr("energy", 0), 0, MAX_ENERGY));
        progress = Math.max(0, input.getIntOr("progress", 0));
        maxProgress = Math.max(1, input.getIntOr("maxProgress", ArcFusionRecipe.DEFAULT_TIME));
        energyPerTick = Math.max(0, input.getIntOr("energyPerTick", 0));
        active = input.getBooleanOr("active", false);
        sideConfig.load(input, "sideConfig");

        // Leere Slots werden nicht gespeichert, daher vorher leeren
        // (wichtig für Client-Updates, wenn ein Item entnommen wurde).
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);

        clientProgress = progress;
        recipeDirty = true;
    }
}
