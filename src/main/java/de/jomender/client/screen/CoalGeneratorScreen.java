package de.jomender.client.screen;

import de.jomender.blockentity.CoalGeneratorBlockEntity;
import de.jomender.client.gui.CoalGeneratorLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.menu.CoalGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

public class CoalGeneratorScreen extends JomEnderMachineScreen<CoalGeneratorMenu> {

    private static final int FLAME_BG = 0xFF171722;
    private static final int FLAME_FRAME = 0xFF6D6974;
    private static final int FLAME_HOT = 0xFFFFB13B;
    private static final int FLAME_CORE = 0xFFE5532D;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CoalGeneratorLayout.WIDTH, CoalGeneratorLayout.HEIGHT);
        enableUpgrades(true);
        enableDirectionConfig(true);
        titleLabelX = 10;
        titleLabelY = 7;
        inventoryLabelX = 9;
        inventoryLabelY = CoalGeneratorLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics,
                                  int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        JomEnderGuiTheme.drawPanel(graphics, leftPos, topPos,
                CoalGeneratorLayout.PANEL_WIDTH, CoalGeneratorLayout.HEIGHT);

        // Brennstoff
        JomEnderGuiTheme.drawSlot(graphics,
                leftPos + CoalGeneratorLayout.FUEL_X - 1,
                topPos + CoalGeneratorLayout.FUEL_Y - 1);
        drawFlame(graphics,
                leftPos + CoalGeneratorLayout.FLAME_X,
                topPos + CoalGeneratorLayout.FLAME_Y);

        // Spielerinventar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JomEnderGuiTheme.drawSlot(graphics,
                        leftPos + CoalGeneratorLayout.PLAYER_X + col * 18 - 1,
                        topPos + CoalGeneratorLayout.PLAYER_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + CoalGeneratorLayout.PLAYER_X + col * 18 - 1,
                    topPos + CoalGeneratorLayout.HOTBAR_Y - 1);
        }

        JomEnderGuiTheme.verticalEnergy(graphics,
                leftPos + CoalGeneratorLayout.ENERGY_X,
                topPos + CoalGeneratorLayout.ENERGY_Y,
                CoalGeneratorLayout.ENERGY_W, CoalGeneratorLayout.ENERGY_H,
                menu.getEnergy(), CoalGeneratorBlockEntity.MAX_ENERGY);

        drawJomEnderPopups(graphics);
    }

    /** Flamme wie beim Ofen: füllt sich von unten, solange Brennstoff brennt. */
    private void drawFlame(GuiGraphicsExtractor g, int x, int y) {
        int s = CoalGeneratorLayout.FLAME_SIZE;
        g.fill(x, y, x + s, y + s, FLAME_FRAME);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, FLAME_BG);

        int burn = menu.getBurnTime();
        int max = menu.getMaxBurnTime();
        if (burn <= 0 || max <= 0) return;

        int inner = s - 4;
        int h = Math.max(1, (int) ((long) Math.min(burn, max) * inner / max));
        int bottom = y + s - 2;
        g.fill(x + 2, bottom - h, x + s - 2, bottom, FLAME_HOT);
        // Glut-Kern in der Mitte
        int coreH = Math.max(1, h / 2);
        g.fill(x + 4, bottom - coreH, x + s - 4, bottom, FLAME_CORE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, JomEnderGuiTheme.TITLE_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                JomEnderGuiTheme.TEXT_COLOR, false);

        boolean burning = menu.getBurnTime() > 0;
        int x = CoalGeneratorLayout.STATUS_X;
        int y = CoalGeneratorLayout.STATUS_Y;
        graphics.text(font, Component.literal(burning ? "Burning" : "Idle"), x, y,
                burning ? JomEnderGuiTheme.TITLE_COLOR : JomEnderGuiTheme.TEXT_COLOR, false);
        graphics.text(font, Component.literal(
                        (burning ? menu.getGenerationPerTick() : 0) + " FE/t"),
                x, y + 12, JomEnderGuiTheme.TEXT_COLOR, false);
        if (burning) {
            graphics.text(font, Component.literal((menu.getBurnTime() + 19) / 20 + "s"),
                    x, y + 24, JomEnderGuiTheme.TEXT_COLOR, false);
        }
    }

    @Override
    protected void onUpgradePopupChanged(boolean open) {
        menu.setUpgradePopupOpen(open);
    }

    @Override
    protected void onUpgradePopupMoved(int x, int y) {
        menu.moveUpgradeSlots(x + 18, y + 27);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + CoalGeneratorLayout.ENERGY_X;
        int y = topPos + CoalGeneratorLayout.ENERGY_Y;
        if (mouseX >= x && mouseX < x + CoalGeneratorLayout.ENERGY_W
                && mouseY >= y && mouseY < y + CoalGeneratorLayout.ENERGY_H) {
            graphics.setTooltipForNextFrame(Component.literal(
                    menu.getEnergy() + " / " + CoalGeneratorBlockEntity.MAX_ENERGY + " FE"),
                    mouseX, mouseY);
        }
    }
}
