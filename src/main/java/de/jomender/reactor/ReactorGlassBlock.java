package de.jomender.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Reaktor-Glas: verbundenes Glas, auch außerhalb des Reaktors (z.B. für Fenster in der Base).
 *
 * Normal:     verbindet sich mit angrenzendem Reaktor-Glas.
 * Im Reaktor: (formed = true) verbindet sich mit allen Reaktor-Blöcken der Hülle.
 * Im Reaktor nur in Wandflächen erlaubt, nicht an Kanten.
 */
public class ReactorGlassBlock extends TransparentBlock {
    public ReactorGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(ReactorParts.defaults(stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        ReactorParts.addProperties(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withGlassConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        return state.setValue(ReactorParts.connection(direction), connects(state, neighbourState));
    }

    /** Verbindung in eine Richtung: immer zu Reaktor-Glas, im fertigen Reaktor auch zu den anderen Teilen. */
    private boolean connects(BlockState self, BlockState neighbour) {
        if (neighbour.is(this)) return true;
        return self.getValue(ReactorParts.FORMED) && ReactorStructure.isReactorPart(neighbour);
    }

    /** Verbindungen nur zu benachbartem Reaktor-Glas (Zustand außerhalb eines Reaktors). */
    public static BlockState withGlassConnections(BlockState state, BlockGetter level, BlockPos pos) {
        state = state.setValue(ReactorParts.FORMED, false);
        for (Direction direction : Direction.values()) {
            boolean glass = level.getBlockState(pos.relative(direction)).is(state.getBlock());
            state = state.setValue(ReactorParts.connection(direction), glass);
        }
        return state;
    }
}
