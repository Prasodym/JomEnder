
package de.jomender.client.screen;

import de.jomender.menu.CoalGeneratorMenu;
import de.jomender.blockentity.CoalGeneratorBlockEntity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

public class CoalGeneratorScreen
        extends AbstractContainerScreen<CoalGeneratorMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int BAR_X = 49;
    private static final int BAR_Y = 43;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 10;

    public CoalGeneratorScreen(
            CoalGeneratorMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);

        this.titleLabelX = 9;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        // =========================
        // MASCHINENGEHÄUSE
        // =========================

        graphics.fill(
                x, y,
                x + GUI_WIDTH, y + GUI_HEIGHT,
                0xFF242D35
        );

        graphics.fill(
                x + 3, y + 3,
                x + 173, y + 163,
                0xFF35434D
        );

        // Oberer Maschinenbereich
        graphics.fill(
                x + 7, y + 17,
                x + 169, y + 69,
                0xFF18252B
        );

        // =========================
        // BRENNSTOFF-SLOT
        // =========================

        drawSlot(graphics, x + 22, y + 34);

        // =========================
        // VIER UPGRADE-SLOTS
        // =========================

        for (int i = 0; i < 4; i++) {
            int slotX = x + 118 + (i % 2) * 22;
            int slotY = y + 24 + (i / 2) * 22;

            drawSlot(graphics, slotX, slotY);
        }

        // =========================
        // LIVE-ENERGIEBALKEN
        // =========================

        drawEnergyBar(graphics, x, y);

        // =========================
        // SPIELERINVENTAR
        // =========================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {

                drawSlot(
                        graphics,
                        x + 7 + col * 18,
                        y + 83 + row * 18
                );
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            drawSlot(
                    graphics,
                    x + 7 + col * 18,
                    y + 141
            );
        }
    }

    // =========================
    // ROTER ENERGIEBALKEN
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


    private void drawEnergyBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        int barX = x + BAR_X;
        int barY = y + BAR_Y;

        int energy = menu.getEnergy();
        int maxEnergy = CoalGeneratorBlockEntity.MAX_ENERGY;

        int innerWidth = BAR_WIDTH - 4;

        int filled = Math.clamp(
                (int) ((energy / (float) maxEnergy) * innerWidth),
                0,
                innerWidth
        );

        // Silberner Außenrahmen
        graphics.fill(
                barX - 1,
                barY - 1,
                barX + BAR_WIDTH + 1,
                barY + BAR_HEIGHT + 1,
                0xFFBFCED6
        );

        // Dunkler Innenrahmen
        graphics.fill(
                barX,
                barY,
                barX + BAR_WIDTH,
                barY + BAR_HEIGHT,
                0xFF11171C
        );

        // Dunkelroter Hintergrund
        graphics.fill(
                barX + 2,
                barY + 2,
                barX + BAR_WIDTH - 2,
                barY + BAR_HEIGHT - 2,
                0xFF300C10
        );

        // Rote Energie-Füllung
        if (filled > 0) {
            graphics.fill(
                    barX + 2,
                    barY + 2,
                    barX + 2 + filled,
                    barY + BAR_HEIGHT - 2,
                    0xFFE53935
            );
        }
    }

    // =========================
    // ENERGIE-TOOLTIP
    // =========================

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(
                graphics, mouseX, mouseY, partialTick
        );

        int barX = leftPos + BAR_X;
        int barY = topPos + BAR_Y;

        boolean hovering =
                mouseX >= barX - 1 &&
                        mouseX < barX + BAR_WIDTH + 1 &&
                        mouseY >= barY - 1 &&
                        mouseY < barY + BAR_HEIGHT + 1;

        if (hovering) {
            graphics.setTooltipForNextFrame(
                    Component.literal(
                            menu.getEnergy()
                                    + " / "
                                    + CoalGeneratorBlockEntity.MAX_ENERGY
                                    + " FE"
                    ),
                    mouseX,
                    mouseY
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
        // Heller Außenrahmen
        graphics.fill(
                x, y,
                x + 18, y + 18,
                0xFF718A91
        );

        // Dunkler Hintergrund
        graphics.fill(
                x + 1, y + 1,
                x + 17, y + 17,
                0xFF19252D
        );
    }
}
