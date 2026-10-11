package de.jomender.client.gui;

/** Pixel coordinates for the Arc Fusion Chamber, shared by Screen and Menu. */
public final class ArcFusionLayout {
    private ArcFusionLayout() {}

    public static final int WIDTH = JomEnderGuiLayout.WIDTH;
    public static final int PANEL_WIDTH = JomEnderGuiLayout.PANEL_WIDTH;
    public static final int HEIGHT = 192;

    // Zwei Inputs oben, Pfeil nach unten, zwei Outputs unten (Item-Positionen).
    public static final int INPUT_X = 57;
    public static final int INPUT_Y = 24;
    public static final int SLOT_GAP = 28;
    public static final int PROGRESS_X = 63;
    public static final int PROGRESS_Y = 42;
    public static final int OUTPUT_X = 57;
    public static final int OUTPUT_Y = 76;

    // Status-Text links
    public static final int STATUS_X = 10;
    public static final int STATUS_Y = 26;

    // Vertikaler Energiebalken
    public static final int ENERGY_X = 151;
    public static final int ENERGY_Y = 22;
    public static final int ENERGY_W = 10;
    public static final int ENERGY_H = 70;

    // Upgrade-Slots: Startposition, solange das Popup geschlossen ist
    public static final int UPGRADE_X = 54;
    public static final int UPGRADE_Y = 159;
    public static final int UPGRADE_GAP = JomEnderGuiLayout.UPGRADE_GAP;

    // Spielerinventar
    public static final int INVENTORY_LABEL_Y = 98;
    public static final int PLAYER_X = JomEnderGuiLayout.PLAYER_X;
    public static final int PLAYER_Y = 110;
    public static final int HOTBAR_Y = 168;
}
