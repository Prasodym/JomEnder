package de.jomender.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared textures for every JomEnder machine GUI. */
public final class JomEnderGuiTheme {
    private JomEnderGuiTheme() {}

    public static final int TITLE_COLOR = 0xFF35DD8B;
    public static final int TEXT_COLOR = 0xFFE0E4E8;
    public static final int ENERGY_COLOR = 0xFF19F79B;
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/gui_base.png");
    private static final Identifier SLOT = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/jomender_slot.png");
    private static final Identifier ARROW = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/smelter_progress_arrow.png");

    // Shared miner button sprites; other machines may reuse the same style.
    private static final Identifier BUTTON_NORMAL = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/default_button.png");
    private static final Identifier BUTTON_HOVER = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/default_button_hover.png");
    private static final Identifier BUTTON_PRESSED = Identifier.fromNamespaceAndPath(
            "jomender", "textures/gui/default_button_press.png");

    public static void drawMachineButton(GuiGraphicsExtractor g, int x, int y,
                                         int width, int height, boolean pressed, boolean hovered) {
        Identifier texture = pressed ? BUTTON_PRESSED : hovered ? BUTTON_HOVER : BUTTON_NORMAL;
        drawNineSlice(g, texture, x, y, width, height, BUTTON_TEX_W, BUTTON_TEX_H, BUTTON_BORDER);
    }

    private static final int BUTTON_TEX_W = 200, BUTTON_TEX_H = 20, BUTTON_BORDER = 2;

    /**
     * Zeichnet eine Textur als 9-Slice: Ecken bleiben unverzerrt,
     * Ränder und Mitte werden auf die Zielgröße gestreckt.
     */
    public static void drawNineSlice(GuiGraphicsExtractor g, Identifier tex, int x, int y,
                                     int w, int h, int texW, int texH, int b) {
        int midW = w - 2 * b, midH = h - 2 * b;
        int srcMidW = texW - 2 * b, srcMidH = texH - 2 * b;
        // Ecken
        part(g, tex, x,         y,         b, b, 0,        0,        b, b, texW, texH);
        part(g, tex, x + w - b, y,         b, b, texW - b, 0,        b, b, texW, texH);
        part(g, tex, x,         y + h - b, b, b, 0,        texH - b, b, b, texW, texH);
        part(g, tex, x + w - b, y + h - b, b, b, texW - b, texH - b, b, b, texW, texH);
        // Ränder
        part(g, tex, x + b,     y,         midW, b, b,        0,        srcMidW, b, texW, texH);
        part(g, tex, x + b,     y + h - b, midW, b, b,        texH - b, srcMidW, b, texW, texH);
        part(g, tex, x,         y + b,     b, midH, 0,        b,        b, srcMidH, texW, texH);
        part(g, tex, x + w - b, y + b,     b, midH, texW - b, b,        b, srcMidH, texW, texH);
        // Mitte
        part(g, tex, x + b,     y + b,     midW, midH, b,     b,        srcMidW, srcMidH, texW, texH);
    }

    private static void part(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h,
                             int u, int v, int srcW, int srcH, int texW, int texH) {
        if (w <= 0 || h <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, srcW, srcH, texW, texH);
    }

    /** Render 25x25 gui_base.png as a true 9-slice. Source corners are never stretched. */
    public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        if (width < 8 || height < 8) return;
        final int c = 4;
        final int center = 25 - 2 * c;
        int[] dx = { x, x+c, x+width-c };
        int[] dy = { y, y+c, y+height-c };
        int[] dw = { c, width-2*c, c };
        int[] dh = { c, height-2*c, c };
        int[] uv = {0, c, 25-c};
        int[] sw = {c, center, c};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                g.blit(RenderPipelines.GUI_TEXTURED, PANEL, dx[col], dy[row],
                        (float)uv[col], (float)uv[row], dw[col], dh[row],
                        sw[col], sw[row], 25, 25);
            }
        }
    }

    public static void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    /** Energy grows upwards; frame is shared, position is chosen by each machine. */
    public static void verticalEnergy(GuiGraphicsExtractor g, int x, int y,
                                      int width, int height, int amount, int max) {
        g.fill(x, y, x + width, y + height, 0xFF6D6974);
        g.fill(x+1, y+1, x+width-1, y+height-1, 0xFF171722);
        int inner = height - 4;
        int fill = max <= 0 ? 0 : (int)((long)Math.clamp(amount,0,max)*inner/max);
        if (fill > 0) g.fill(x+2, y+height-2-fill, x+width-2, y+height-2, ENERGY_COLOR);
    }

    /** Original 32x32 MCGUI Crafter sprite. Base arrow plus top-to-bottom progress fill. */
    public static void downwardProgress(GuiGraphicsExtractor g, int x, int y,
                                        int progress, int max) {
        g.blit(RenderPipelines.GUI_TEXTURED, ARROW, x, y, 0, 0, 32, 32, 32, 32);
        int filled = max <= 0 ? 0 : (int)((long)Math.clamp(progress,0,max)*32/max);
        if (filled > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, ARROW, x, y, 0, 0,
                    32, filled, 32, filled, 32, 32, 0xFF40D5A6);
        }
    }
}
