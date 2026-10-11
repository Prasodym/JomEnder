package de.jomender.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Seiten relativ zur Vorderseite der Maschine, so wie der Spieler sie sieht. */
public enum RelativeSide {
    FRONT("Front", "F"),
    BACK("Back", "B"),
    LEFT("Left", "L"),
    RIGHT("Right", "R"),
    TOP("Top", "T"),
    BOTTOM("Bottom", "D");

    private final String name;
    private final String shortName;

    RelativeSide(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public Component displayName() { return Component.literal(name); }
    public String shortName() { return shortName; }

    /**
     * Wandelt in eine Weltrichtung um. "Links" ist links aus Sicht eines Spielers,
     * der vor der Maschine steht und auf ihre Vorderseite schaut.
     */
    public Direction toAbsolute(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }
}
