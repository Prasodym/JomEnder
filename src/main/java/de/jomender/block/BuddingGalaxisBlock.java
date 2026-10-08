package de.jomender.block;

import de.jomender.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;

public class BuddingGalaxisBlock extends BuddingAmethystBlock {
    public BuddingGalaxisBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level,
                              BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;

        Direction face = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(face);
        BlockState current = level.getBlockState(targetPos);
        BlockState next;

        if (BuddingAmethystBlock.canClusterGrowAtState(current)) {
            next = ModBlocks.SMALL_GALAXIS_BUD.get().defaultBlockState();
        } else if (current.is(ModBlocks.SMALL_GALAXIS_BUD.get())
                && current.getValue(AmethystClusterBlock.FACING) == face) {
            next = ModBlocks.MEDIUM_GALAXIS_BUD.get().defaultBlockState();
        } else if (current.is(ModBlocks.MEDIUM_GALAXIS_BUD.get())
                && current.getValue(AmethystClusterBlock.FACING) == face) {
            next = ModBlocks.LARGE_GALAXIS_BUD.get().defaultBlockState();
        } else if (current.is(ModBlocks.LARGE_GALAXIS_BUD.get())
                && current.getValue(AmethystClusterBlock.FACING) == face) {
            next = ModBlocks.GALAXIS_CLUSTER.get().defaultBlockState();
        } else {
            return;
        }

        level.setBlock(targetPos,
                next.setValue(AmethystClusterBlock.FACING, face)
                    .setValue(AmethystClusterBlock.WATERLOGGED,
                            current.getFluidState().is(FluidTags.WATER)),
                3);
    }
}
