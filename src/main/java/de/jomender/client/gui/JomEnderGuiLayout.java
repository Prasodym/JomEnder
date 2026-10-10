package de.jomender.client.gui;

/** Pixel coordinates shared by the Screen and Menu (MCGUI Crafter export). */
public final class JomEnderGuiLayout {
    private JomEnderGuiLayout() {}

    // MCGUI export uses -111..169 for the whole panel; shift Y by +111.
    public static final int WIDTH = 200;
    public static final int HEIGHT = 280;
    public static final int PANEL_WIDTH = 177;
    public static final int BUTTON_X = 178;
    public static final int BUTTON_U_Y = 3;
    public static final int BUTTON_D_Y = 24;
    public static final int INPUT_X = 60; // visual frame starts at X=59
    public static final int INPUT_Y = 30; // visual frame starts at Y=29
    public static final int INPUT_GAP = 21;
    public static final int OUTPUT_X = 54;
    public static final int OUTPUT_Y = 94;
    public static final int OUTPUT_GAP = 18;
    public static final int PLAYER_X = 9;
    public static final int PLAYER_Y = 196;
    public static final int HOTBAR_Y = 254;
    // Existing 4 upgrades remain accessible during the visual transition.
    public static final int UPGRADE_X = 54;
    public static final int UPGRADE_Y = 159;
    public static final int UPGRADE_GAP = 22;
    public static final int PROGRESS_X = 73;
    public static final int PROGRESS_Y = 53;
    public static final int PROGRESS_W = 32;
    public static final int PROGRESS_H = 28;
    public static final int ENERGY_X = 151;
    public static final int ENERGY_Y = 31;
    public static final int ENERGY_W = 10;
    public static final int ENERGY_H = 97;
}
