package de.jomender.reactor;

import de.jomender.ModBlockEntities;
import de.jomender.ModEnergyTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Reaktor-Anschluss: reicht je nach Modus Items/Flüssigkeit/Strom an den Controller weiter.
 * Der Controller trägt sich hier ein, sobald der Reaktor fertig gebaut ist.
 */
public class ReactorPortBlockEntity extends BlockEntity {
    /** Max. FE, die ein Anschluss pro Tick an Nachbarn abgibt. */
    public static final int MAX_ENERGY_PUSH = 100_000;

    private @Nullable BlockPos controllerPos;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_PORT.get(), pos, state);
    }

    public PortMode getMode() {
        return getBlockState().getValue(ReactorPortBlock.MODE);
    }

    /** Wird vom Controller gesetzt (null = nicht mehr Teil eines Reaktors). */
    void setController(@Nullable BlockPos pos) {
        if (java.util.Objects.equals(controllerPos, pos)) return;
        controllerPos = pos;
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    public @Nullable ReactorControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity controller
                && controller.isFormed() && controller.hasPort(worldPosition)) {
            return controller;
        }
        return null;
    }

    // =========================
    // CAPABILITIES (registriert in ModCapabilities)
    // =========================

    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        ReactorControllerBlockEntity controller = getController();
        if (controller == null) return null;
        return switch (getMode()) {
            case INPUT -> controller.getFuelInput();
            case WASTE -> controller.getWasteOutput();
            case ENERGY -> null;
        };
    }

    public @Nullable ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        ReactorControllerBlockEntity controller = getController();
        return controller != null && getMode() == PortMode.INPUT ? controller.getWaterTank() : null;
    }

    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        ReactorControllerBlockEntity controller = getController();
        return controller != null && getMode() == PortMode.ENERGY ? controller.getEnergyStorage() : null;
    }

    // =========================
    // AKTIV HERAUSSCHIEBEN
    // =========================

    void serverTick() {
        if (level == null || level.getGameTime() % 2 != 0) return;
        ReactorControllerBlockEntity controller = getController();
        if (controller == null) return;

        switch (getMode()) {
            case ENERGY -> ModEnergyTransfer.sendToNeighbors(level, worldPosition,
                    controller.getEnergyStorage(), MAX_ENERGY_PUSH * 2,
                    side -> !isReactorNeighbor(side));
            case WASTE -> pushWaste(controller);
            case INPUT -> { }
        }
    }

    private boolean isReactorNeighbor(Direction side) {
        return level != null && ReactorStructure.isReactorPart(level.getBlockState(worldPosition.relative(side)));
    }

    private void pushWaste(ReactorControllerBlockEntity controller) {
        ItemStack waste = controller.getItem(ReactorControllerBlockEntity.WASTE_SLOT);
        if (waste.isEmpty() || level == null) return;

        for (Direction side : Direction.values()) {
            if (waste.isEmpty()) return;
            if (isReactorNeighbor(side)) continue;
            BlockPos target = worldPosition.relative(side);
            if (!level.isLoaded(target)) continue;

            ResourceHandler<ItemResource> handler =
                    level.getCapability(Capabilities.Item.BLOCK, target, side.getOpposite());
            if (handler == null) continue;

            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(ItemResource.of(waste), waste.getCount(), transaction);
                if (inserted > 0) {
                    transaction.commit();
                    controller.removeItem(ReactorControllerBlockEntity.WASTE_SLOT, inserted);
                }
            }
        }
    }
}
