package de.jomender.reactor;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Gemeinsame Blockzustände aller Reaktor-Teile für die "verbundene" Optik.
 *
 * formed = true  -> Block gehört zu einem fertigen Reaktor, eigener Rand ausgeblendet
 * north/east/... -> in diese Richtung schließt ein anderer Reaktorblock an
 *                   (dort wird KEIN Rand gezeichnet, siehe blockstates/*.json)
 */
public final class ReactorParts {
    private ReactorParts() {}

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public static BooleanProperty connection(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case EAST -> BlockStateProperties.EAST;
            case WEST -> BlockStateProperties.WEST;
            case UP -> BlockStateProperties.UP;
            case DOWN -> BlockStateProperties.DOWN;
        };
    }

    public static void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
        for (Direction direction : Direction.values()) builder.add(connection(direction));
    }

    /** Standard: nicht geformt, keine Verbindungen. */
    public static BlockState defaults(BlockState state) {
        state = state.setValue(FORMED, false);
        for (Direction direction : Direction.values()) state = state.setValue(connection(direction), false);
        return state;
    }

    public static boolean hasProperties(BlockState state) {
        return state.hasProperty(FORMED);
    }
}
