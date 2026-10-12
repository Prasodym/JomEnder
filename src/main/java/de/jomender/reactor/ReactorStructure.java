package de.jomender.reactor;

import de.jomender.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Erkennt den Reaktor-Multiblock rund um den Controller.
 *
 * Regeln:
 *  - Quader von 3x3x3 bis 7x7x7 (jede Seite einzeln 3..7)
 *  - Kanten und Ecken: nur Reaktor-Gehäuse
 *  - Wandflächen: Gehäuse, Anschlüsse, Reaktor-Glas (oder normales Glas) und genau EIN Controller
 *  - Innenraum: komplett leer (Luft)
 *  Je größer der Innenraum, desto mehr Leistung.
 */
public final class ReactorStructure {
    private ReactorStructure() {}

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 7;

    /** Warum der Reaktor nicht steht - wird im GUI angezeigt. */
    public enum Problem {
        NONE("formed"),
        NO_INTERIOR("no_interior"),
        BAD_SIZE("bad_size"),
        BAD_EDGE("bad_edge"),
        BAD_WALL("bad_wall"),
        INTERIOR_BLOCKED("interior_blocked"),
        SECOND_CONTROLLER("second_controller");

        private final String key;
        Problem(String key) { this.key = key; }
        public String translationKey() { return "gui.jomender.reactor.problem." + key; }
    }

    /** Ergebnis einer Prüfung. Bei Erfolg sind min/max/ports gefüllt. */
    public record Result(Problem problem, @Nullable BlockPos where,
                         BlockPos min, BlockPos max, List<BlockPos> ports) {
        public boolean formed() { return problem == Problem.NONE; }
        public int sizeX() { return max.getX() - min.getX() + 1; }
        public int sizeY() { return max.getY() - min.getY() + 1; }
        public int sizeZ() { return max.getZ() - min.getZ() + 1; }
        /** Anzahl der Innenraum-Blöcke (1 bei 3x3x3, 125 bei 7x7x7). */
        public int interiorVolume() { return (sizeX() - 2) * (sizeY() - 2) * (sizeZ() - 2); }

        static Result fail(Problem problem, @Nullable BlockPos where) {
            return new Result(problem, where, BlockPos.ZERO, BlockPos.ZERO, List.of());
        }
    }

    public static Result check(Level level, BlockPos controller) {
        Result best = Result.fail(Problem.NO_INTERIOR, null);

        // Der Innenraum kann in jeder Richtung hinter dem Controller liegen
        for (Direction inward : Direction.values()) {
            if (!level.getBlockState(controller.relative(inward)).isAir()) continue;

            Result result = tryDirection(level, controller, inward);
            if (result.formed()) return result;
            // Den "weitesten" Fehler merken, damit das GUI etwas Sinnvolles anzeigt
            if (result.problem().ordinal() > best.problem().ordinal()) best = result;
        }
        return best;
    }

    private static Result tryDirection(Level level, BlockPos controller, Direction inward) {
        // Gegenüberliegende Wand suchen
        int depth = -1;
        for (int k = 2; k < MAX_SIZE; k++) {
            if (isReactorPart(level.getBlockState(controller.relative(inward, k)))) {
                depth = k;
                break;
            }
        }
        if (depth < 0) return Result.fail(Problem.BAD_SIZE, null);

        // Die zwei Achsen der Controller-Wand
        Direction[] wallAxes = otherAxes(inward.getAxis());
        BlockPos min = controller, max = controller;
        for (Direction axis : wallAxes) {
            int pos = extent(level, controller, axis);
            int neg = extent(level, controller, axis.getOpposite());
            min = min(min, controller.relative(axis.getOpposite(), neg));
            max = max(max, controller.relative(axis, pos));
        }
        BlockPos far = controller.relative(inward, depth);
        min = min(min, far);
        max = max(max, far);

        int sx = max.getX() - min.getX() + 1, sy = max.getY() - min.getY() + 1, sz = max.getZ() - min.getZ() + 1;
        if (!inRange(sx) || !inRange(sy) || !inRange(sz)) return Result.fail(Problem.BAD_SIZE, null);

        return validateBox(level, controller, min, max);
    }

    private static Result validateBox(Level level, BlockPos controller, BlockPos min, BlockPos max) {
        List<BlockPos> ports = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    pos.set(x, y, z);
                    int borders = (x == min.getX() || x == max.getX() ? 1 : 0)
                            + (y == min.getY() || y == max.getY() ? 1 : 0)
                            + (z == min.getZ() || z == max.getZ() ? 1 : 0);
                    BlockState state = level.getBlockState(pos);

                    if (borders == 0) {
                        if (!state.isAir()) return Result.fail(Problem.INTERIOR_BLOCKED, pos.immutable());
                    } else if (borders >= 2) {
                        if (!state.is(ModBlocks.REACTOR_CASING.get())) return Result.fail(Problem.BAD_EDGE, pos.immutable());
                    } else {
                        if (state.is(ModBlocks.REACTOR_CONTROLLER.get())) {
                            if (!pos.equals(controller)) return Result.fail(Problem.SECOND_CONTROLLER, pos.immutable());
                        } else if (state.is(ModBlocks.REACTOR_PORT.get())) {
                            ports.add(pos.immutable());
                        } else if (!state.is(ModBlocks.REACTOR_CASING.get())
                                && !state.is(ModBlocks.REACTOR_GLASS.get())
                                && !state.is(Tags.Blocks.GLASS_BLOCKS)) {
                            return Result.fail(Problem.BAD_WALL, pos.immutable());
                        }
                    }
                }
            }
        }
        return new Result(Problem.NONE, null, min, max, List.copyOf(ports));
    }

    /** Wie weit geht die Wand von start aus in Richtung dir weiter? */
    private static int extent(Level level, BlockPos start, Direction dir) {
        int n = 0;
        while (n < MAX_SIZE - 1 && isReactorPart(level.getBlockState(start.relative(dir, n + 1)))) n++;
        return n;
    }

    /** Liegt pos auf der Hülle (Wand/Kante/Ecke) der Box min..max? */
    public static boolean isOnShell(BlockPos pos, BlockPos min, BlockPos max) {
        boolean inside = pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        boolean border = pos.getX() == min.getX() || pos.getX() == max.getX()
                || pos.getY() == min.getY() || pos.getY() == max.getY()
                || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
        return inside && border;
    }

    public static boolean isReactorPart(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get())
                || state.is(ModBlocks.REACTOR_CONTROLLER.get())
                || state.is(ModBlocks.REACTOR_PORT.get())
                || state.is(ModBlocks.REACTOR_GLASS.get())
                || state.is(Tags.Blocks.GLASS_BLOCKS);
    }

    private static boolean inRange(int size) {
        return size >= MIN_SIZE && size <= MAX_SIZE;
    }

    private static Direction[] otherAxes(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction[]{Direction.UP, Direction.SOUTH};
            case Y -> new Direction[]{Direction.EAST, Direction.SOUTH};
            case Z -> new Direction[]{Direction.EAST, Direction.UP};
        };
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }
}
