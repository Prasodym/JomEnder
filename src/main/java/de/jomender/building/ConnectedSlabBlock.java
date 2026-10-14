package de.jomender.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

/**
 * Stufe mit verbundener Textur. Verbindet sich seitlich mit Stufen derselben Sorte und Höhe
 * (unten/oben). Doppelte Stufen verhalten sich wie der volle Block.
 */
public class ConnectedSlabBlock extends SlabBlock {

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private final String family;

    public ConnectedSlabBlock(String family, Properties properties) {
        super(properties);
        this.family = family;
        BlockState state = defaultBlockState();
        for (Direction d : HORIZONTAL) state = state.setValue(ConnectedBlock.property(d), false);
        registerDefaultState(state);
    }

    public String family() {
        return family;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        for (Direction d : HORIZONTAL) builder.add(ConnectedBlock.property(d));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        if (direction.getAxis().isHorizontal() && updated.is(this)) {
            updated = updated.setValue(ConnectedBlock.property(direction), connects(updated, neighbourState));
        }
        return updated;
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction d : HORIZONTAL) {
            state = state.setValue(ConnectedBlock.property(d), connects(state, level.getBlockState(pos.relative(d))));
        }
        return state;
    }

    /** Gleiche Sorte und gleiche Höhe; doppelte Stufen verbinden sich auch mit dem vollen Block. */
    private boolean connects(BlockState self, BlockState neighbour) {
        SlabType type = self.getValue(TYPE);
        if (neighbour.getBlock() instanceof ConnectedSlabBlock other) {
            return other.family.equals(family) && neighbour.getValue(TYPE) == type;
        }
        if (type == SlabType.DOUBLE && neighbour.getBlock() instanceof ConnectedBlock block) {
            return block.family().equals(family);
        }
        return false;
    }
}
