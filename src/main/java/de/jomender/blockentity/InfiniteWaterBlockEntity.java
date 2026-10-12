package de.jomender.blockentity;

import de.jomender.ModBlockEntities;
import de.jomender.block.InfiniteWaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.InfiniteResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Unendlich Wasser ohne Transfer-Limit.
 *
 * Passiv: Fluid-Capability auf allen Seiten - jede Pipe (Mekanism, EnderIO, ...) kann so viel
 *         herausziehen, wie sie will. Hineingepumptes Wasser verschwindet einfach.
 * Aktiv:  schiebt jeden Tick Wasser in angrenzende Tanks/Maschinen (z.B. den Reaktor-Anschluss).
 */
public class InfiniteWaterBlockEntity extends BlockEntity {
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    /** "Ohne Limit": so viel, wie das Ziel nehmen kann. Halbes int-Maximum, damit fremde Mods nicht überlaufen. */
    private static final int PUSH_AMOUNT = Integer.MAX_VALUE / 2;

    private static final ResourceHandler<FluidResource> HANDLER = new InfiniteResourceHandler<>(WATER);

    public InfiniteWaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_WATER.get(), pos, state);
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return HANDLER;
    }

    public void serverTick() {
        if (level == null) return;
        for (Direction side : Direction.values()) {
            BlockPos target = worldPosition.relative(side);
            if (!level.isLoaded(target) || level.getBlockState(target).getBlock() instanceof InfiniteWaterBlock) {
                continue;
            }
            ResourceHandler<FluidResource> handler =
                    level.getCapability(Capabilities.Fluid.BLOCK, target, side.getOpposite());
            if (handler == null) continue;

            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.insert(WATER, PUSH_AMOUNT, transaction) > 0) {
                    transaction.commit();
                }
            }
        }
    }
}
