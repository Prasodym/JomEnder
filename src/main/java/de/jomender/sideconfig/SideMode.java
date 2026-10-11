package de.jomender.sideconfig;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Was eine Maschinenseite nach außen darf. */
public enum SideMode {
    NONE("Off", 0xFF5A5A66),
    INPUT("Input", 0xFF3B82F6),
    OUTPUT("Output", 0xFFF59E0B),
    BOTH("In & Out", 0xFF35DD8B);

    private static final SideMode[] VALUES = values();

    private final String label;
    private final int color;

    SideMode(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public boolean canInput() { return this == INPUT || this == BOTH; }
    public boolean canOutput() { return this == OUTPUT || this == BOTH; }

    public SideMode next() { return VALUES[(ordinal() + 1) % VALUES.length]; }
    public SideMode previous() { return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length]; }

    public MutableComponent label() { return Component.literal(label); }
    /** ARGB-Farbe für Kachelrahmen und Legende. */
    public int color() { return color; }

    public static SideMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}
