
package de.jomender.block;

import de.jomender.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import de.jomender.blockentity.GermaniumMinerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class GermaniumMinerPartBlock extends Block {

    public static final IntegerProperty OFFSET_X =
            IntegerProperty.create("offset_x", 0, 2);

    public static final IntegerProperty OFFSET_Y =
            IntegerProperty.create("offset_y", 0, 2);

    public static final IntegerProperty OFFSET_Z =
            IntegerProperty.create("offset_z", 0, 2);

    public GermaniumMinerPartBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(OFFSET_X, 1)
                        .setValue(OFFSET_Y, 0)
                        .setValue(OFFSET_Z, 1)
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            BlockPos controllerPos = getControllerPos(pos, state);

            if (level.getBlockEntity(controllerPos)
                    instanceof GermaniumMinerBlockEntity miner) {
                serverPlayer.openMenu(miner);
            }
        }

        return InteractionResult.SUCCESS;
    }
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    public static BlockPos getControllerPos(
            BlockPos partPos,
            BlockState state
    ) {
        return partPos.offset(
                1 - state.getValue(OFFSET_X),
                -state.getValue(OFFSET_Y),
                1 - state.getValue(OFFSET_Z)
        );
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(
                state, level, pos, movedByPiston
        );

        BlockPos controllerPos = getControllerPos(pos, state);

        // Entfernt man ein Strukturteil, wird auch
        // der Controller abgebaut. Dieser räumt
        // anschließend die restliche Struktur auf.
        if (level.getBlockState(controllerPos)
                .is(ModBlocks.GERMANIUM_MINER.get())) {

            level.destroyBlock(controllerPos, true);
        }
    }
}
