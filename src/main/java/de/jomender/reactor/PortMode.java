package de.jomender.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Modus des Reaktor-Anschlusses - wechselt per Rechtsklick. */
public enum PortMode implements StringRepresentable {
    /** Uran (Items) und Wasser (Flüssigkeit) hinein. */
    INPUT("input", ChatFormatting.AQUA),
    /** Strom (FE) hinaus - schiebt aktiv in angrenzende Kabel/Maschinen. */
    ENERGY("energy", ChatFormatting.GREEN),
    /** Atommüll hinaus - schiebt aktiv in angrenzende Kisten/Rohre. */
    WASTE("waste", ChatFormatting.YELLOW);

    private final String name;
    private final ChatFormatting color;

    PortMode(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public PortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component displayName() {
        return Component.translatable("gui.jomender.reactor.port." + name).withStyle(color);
    }
}
