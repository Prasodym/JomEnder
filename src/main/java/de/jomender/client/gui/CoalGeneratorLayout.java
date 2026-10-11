package de.jomender.client.gui;

/** Pixel coordinates for the Coal Generator, shared by Screen and Menu. */
public final class CoalGeneratorLayout {
    private CoalGeneratorLayout() {}

    // Kompakter als der Smelter: nur ein Brennstoff-Slot.
    public static final int WIDTH = JomEnderGuiLayout.WIDTH;        // inkl. U/D-Buttons
    public static final int PANEL_WIDTH = JomEnderGuiLayout.PANEL_WIDTH;
    public static final int HEIGHT = 190;

    // Brennstoff-Slot (Item-Position; der Rahmen liegt 1px weiter links/oben)
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 52;

    // Flammen-Anzeige über dem Brennstoff-Slot
    public static final int FLAME_X = 45;
    public static final int FLAME_Y = 33;
    public static final int FLAME_SIZE = 14;

    // Status-Text
    public static final int STATUS_X = 72;
    public static final int STATUS_Y = 34;

    // Vertikaler Energiebalken
    public static final int ENERGY_X = 151;
    public static final int ENERGY_Y = 22;
    public static final int ENERGY_W = 10;
    public static final int ENERGY_H = 64;

    // Upgrade-Slots: Startposition, solange das Popup geschlossen ist
    public static final int UPGRADE_X = 54;
    public static final int UPGRADE_Y = 159;

    // Spielerinventar
    public static final int INVENTORY_LABEL_Y = 96;
    public static final int PLAYER_X = JomEnderGuiLayout.PLAYER_X;
    public static final int PLAYER_Y = 108;
    public static final int HOTBAR_Y = 166;
}
