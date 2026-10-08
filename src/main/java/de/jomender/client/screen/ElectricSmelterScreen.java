
package de.jomender.client.screen;

import de.jomender.blockentity.ElectricSmelterBlockEntity;
import de.jomender.menu.ElectricSmelterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jspecify.annotations.NonNull;

public class ElectricSmelterScreen
        extends AbstractContainerScreen<ElectricSmelterMenu> {

    // =========================
    // GUI-GRÖSSE
    // =========================

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 198;

    // =========================
    // ENERGIEBALKEN RECHTS
    // =========================

    private static final int ENERGY_BAR_X = 207;
    private static final int ENERGY_BAR_Y = 22;
    private static final int ENERGY_BAR_WIDTH = 9;
    private static final int ENERGY_BAR_HEIGHT = 80;



    public ElectricSmelterScreen(
            ElectricSmelterMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);

        titleLabelX = 9;
        titleLabelY = 7;

        inventoryLabelX = 35;
        inventoryLabelY = 106;


    }

    // =========================
    // HINTERGRUND
    // =========================

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractBackground(
                graphics, mouseX, mouseY, partialTick
        );

        int x = leftPos;
        int y = topPos;

        // Dunkles Metallgehäuse
        graphics.fill(
                x, y,
                x + GUI_WIDTH,
                y + GUI_HEIGHT,
                0xFF242D35
        );

        // Innerer Gehäuserahmen
        graphics.fill(
                x + 3, y + 3,
                x + GUI_WIDTH - 3,
                y + GUI_HEIGHT - 3,
                0xFF35434D
        );

        // Dunkler Maschinenbereich
        graphics.fill(
                x + 7, y + 17,
                x + 223, y + 109,
                0xFF18252B
        );

        // =========================
        // 3 INPUT-SLOTS
        // =========================

        for (int i = 0; i < 3; i++) {
            drawSlot(
                    graphics,
                    x + 24 + i * 22,
                    y + 36
            );
        }

        // =========================
        // 6 OUTPUT-SLOTS
        // =========================

        for (int i = 0; i < 6; i++) {
            drawSlot(
                    graphics,
                    x + 128 + (i % 3) * 22,
                    y + 27 + (i / 3) * 22
            );
        }

        // =========================
        // 4 UPGRADE-SLOTS
        // =========================

        for (int i = 0; i < 4; i++) {
            drawSlot(
                    graphics,
                    x + 76 + i * 22,
                    y + 85
            );
        }

        // =========================
        // SCHMELZFORTSCHRITT
        // =========================

        drawProgressBar(
                graphics,
                x + 96,
                y + 46,
                menu.getProgress(),
                menu.getMaxProgress()
        );

        // =========================
        // VERTIKALER ENERGIEBALKEN
        // =========================

        drawVerticalEnergyBar(
                graphics,
                x,
                y
        );

        // =========================
        // SPIELERINVENTAR
        // =========================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(
                        graphics,
                        x + 34 + col * 18,
                        y + 116 + row * 18
                );
            }
        }

        // =========================
        // HOTBAR
        // =========================

        for (int col = 0; col < 9; col++) {
            drawSlot(
                    graphics,
                    x + 34 + col * 18,
                    y + 174
            );
        }
    }

    // =========================
    // VERTIKALER ENERGIEBALKEN
    // =========================

    private void drawVerticalEnergyBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        int barX = x + ENERGY_BAR_X;
        int barY = y + ENERGY_BAR_Y;

        int energy = Math.clamp(
                menu.getEnergy(),
                0,
                ElectricSmelterBlockEntity.MAX_ENERGY
        );

        int maxEnergy = ElectricSmelterBlockEntity.MAX_ENERGY;

        int width = ENERGY_BAR_WIDTH;
        int height = ENERGY_BAR_HEIGHT;

        // Silberner Außenrahmen
        graphics.fill(
                barX - 1,
                barY - 1,
                barX + width + 1,
                barY + height + 1,
                0xFFBFCED6
        );

        // Dunkler Innenbereich
        graphics.fill(
                barX,
                barY,
                barX + width,
                barY + height,
                0xFF11171C
        );

        // Dunkelroter Hintergrund
        graphics.fill(
                barX + 2,
                barY + 2,
                barX + width - 2,
                barY + height - 2,
                0xFF300C10
        );

        // Energie von unten nach oben auffüllen
        if (maxEnergy > 0 && energy > 0) {

            int innerHeight = height - 4;

            int filled = Math.clamp(
                    (int) (
                            (energy / (float) maxEnergy)
                                    * innerHeight
                    ),
                    0,
                    innerHeight
            );

            if (filled > 0) {
                graphics.fill(
                        barX + 2,
                        barY + height - 2 - filled,
                        barX + width - 2,
                        barY + height - 2,
                        0xFFE53935
                );
            }
        }
    }

    // =========================
    // DEZENTER SCHMELZFORTSCHRITT
    // =========================

    private void drawProgressBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int progress,
            int maxProgress
    ) {
        int width = 25;
        int height = 4;

        // Dunkler Hintergrund
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xFF263B43
        );

        // Türkisfarbener Fortschritt
        if (maxProgress > 0 && progress > 0) {

            int filled = Math.clamp(
                    (int) (
                            (progress / (float) maxProgress)
                                    * width
                    ),
                    0,
                    width
            );

            graphics.fill(
                    x,
                    y,
                    x + filled,
                    y + height,
                    0xFF4CA5AA
            );
        }
    }

    // =========================
    // SLOT-DESIGN
    // =========================

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        // Heller Rahmen
        graphics.fill(
                x,
                y,
                x + 18,
                y + 18,
                0xFF718A91
        );

        // Dunkler Hintergrund
        graphics.fill(
                x + 1,
                y + 1,
                x + 17,
                y + 17,
                0xFF19252D
        );
    }

    // =========================
    // TOOLTIPS
    // =========================

    @Override
    protected void extractLabels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        // Electric Smelter – Türkis
        graphics.text(
                this.font,
                this.title,
                this.titleLabelX,
                this.titleLabelY,
                0xFF4CA5AA,
                false
        );

        // Inventory – Eisblau
        graphics.text(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                0xFFE6F2F5,
                false
        );
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        int energyX = leftPos + ENERGY_BAR_X;
        int energyY = topPos + ENERGY_BAR_Y;

        boolean hoveringEnergy =
                mouseX >= energyX - 1
                        && mouseX < energyX + ENERGY_BAR_WIDTH + 1
                        && mouseY >= energyY - 1
                        && mouseY < energyY + ENERGY_BAR_HEIGHT + 1;

        if (hoveringEnergy) {
            graphics.setTooltipForNextFrame(
                    Component.literal(
                            menu.getEnergy()
                                    + " / "
                                    + ElectricSmelterBlockEntity.MAX_ENERGY
                                    + " FE"
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
