package de.jomender.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Baustein mit verbundener Textur: Zu gleichen Blöcken (und doppelten Stufen derselben Sorte)
 * verschwindet der Rand, nur der äußere Rahmen der ganzen Fläche bleibt.
 * Die Ränder kommen als Overlays aus dem Multipart-Blockstate (north=false -> Rand im Norden).
 */
public class ConnectedBlock extends Block {

    private final String family;

    public ConnectedBlock(String family, Properties properties) {
        super(properties);
        this.family = family;
        BlockState state = stateDefinition.any();
        for (Direction d : Direction.values()) state = state.setValue(property(d), false);
        registerDefaultState(state);
    }

    public String family() {
        return family;
    }

    public static BooleanProperty property(Direction direction) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction d : Direction.values()) builder.add(property(d));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        return state.setValue(property(direction), connects(neighbourState));
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(property(d), connects(level.getBlockState(pos.relative(d))));
        }
        return state;
    }

    /** Verbindet sich mit derselben Sorte: voller Block oder doppelte Stufe. */
    boolean connects(BlockState neighbour) {
        if (neighbour.getBlock() instanceof ConnectedBlock other) {
            return other.family.equals(family);
        }
        if (neighbour.getBlock() instanceof ConnectedSlabBlock slab) {
            return slab.family().equals(family) && neighbour.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
        }
        return false;
    }
}
