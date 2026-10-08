
package de.jomender.client.screen;

import de.jomender.blockentity.GermaniumMinerBlockEntity;
import de.jomender.menu.GermaniumMinerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jspecify.annotations.NonNull;

public class GermaniumMinerScreen
        extends AbstractContainerScreen<GermaniumMinerMenu> {

    private static final int GUI_WIDTH = 245;
    private static final int GUI_HEIGHT = 263;

    private static final int ENERGY_X = 230;
    private static final int ENERGY_Y = 24;
    private static final int ENERGY_W = 7;
    private static final int ENERGY_H = 63;

    private Button startButton;
    private Button silkButton;
    private Button pushButton;

    private EditBox minYInput;
    private EditBox maxYInput;

    public GermaniumMinerScreen(
            GermaniumMinerMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title,
                GUI_WIDTH,
                GUI_HEIGHT
        );

        titleLabelX = 10;
        titleLabelY = 7;

        inventoryLabelX = 23;
        inventoryLabelY = 171;
    }

    @Override
    protected void init() {
        super.init();

        startButton = addRenderableWidget(
                createButton(
                        12, 29, 59, 20,
                        GermaniumMinerMenu.BUTTON_START,
                        "Start"
                )
        );

        silkButton = addRenderableWidget(
                createButton(
                        76, 29, 70, 20,
                        GermaniumMinerMenu.BUTTON_SILK,
                        "Silk: AUS"
                )
        );

        pushButton = addRenderableWidget(
                createButton(
                        151, 29, 70, 20,
                        GermaniumMinerMenu.BUTTON_PUSH,
                        "Push: AUS"
                )
        );

        // Direkte Zahleneingabe für Min Y.
        minYInput = new EditBox(
                font,
                leftPos + 49,
                topPos + 56,
                51,
                18,
                Component.literal("Min Y")
        );
        configureHeightInput(
                minYInput,
                menu.getMinY()
        );
        addRenderableWidget(minYInput);

        // Direkte Zahleneingabe für Max Y.
        maxYInput = new EditBox(
                font,
                leftPos + 154,
                topPos + 56,
                45,
                18,
                Component.literal("Max Y")
        );
        configureHeightInput(
                maxYInput,
                menu.getMaxY()
        );
        addRenderableWidget(maxYInput);

        // Änderungen serverseitig übernehmen.
        addRenderableWidget(
                Button.builder(
                        Component.literal("OK"),
                        button -> applyHeightInputs()
                ).bounds(
                        leftPos + 202,
                        topPos + 56,
                        22,
                        18
                ).build()
        );

        // A und C sind für die nächsten Ausbaustufen
        // vorbereitet. Solange die Serverlogik fehlt,
        // bleiben die Buttons bewusst deaktiviert.
        Button sideConfig = addRenderableWidget(
                createLocalButton(
                        12, 82, 79, 16,
                        "A: Seiten"
                )
        );
        sideConfig.active = false;

        addRenderableWidget(
                Button.builder(
                        Component.literal("C: Filter"),
                        button -> {
                            menu.setFilterPage(!menu.isFilterPage());

                            sendMenuButton(
                                    GermaniumMinerMenu.BUTTON_FILTER_PAGE
                            );
                        }
                ).bounds(
                        leftPos + 96,
                        topPos + 82,
                        79,
                        16
                ).build()
        );

        Button upgrades = addRenderableWidget(
                createLocalButton(
                        180, 82, 44, 16,
                        "B: UPG"
                )
        );
        upgrades.active = false;

        updateButtonLabels();
    }

    private void configureHeightInput(
            EditBox input,
            int initialValue
    ) {
        input.setMaxLength(4);
        input.setFilter(value ->
                value.isEmpty()
                        || value.equals("-")
                        || value.matches("-?\\d{1,3}")
        );
        input.setValue(Integer.toString(initialValue));
    }

    private Button createButton(
            int x,
            int y,
            int width,
            int height,
            int buttonId,
            String label
    ) {
        return Button.builder(
                Component.literal(label),
                button -> sendMenuButton(buttonId)
        ).bounds(
                leftPos + x,
                topPos + y,
                width,
                height
        ).build();
    }

    private Button createLocalButton(
            int x,
            int y,
            int width,
            int height,
            String label
    ) {
        return Button.builder(
                Component.literal(label),
                button -> {
                    // Wird mit der jeweiligen Unter-GUI
                    // implementiert.
                }
        ).bounds(
                leftPos + x,
                topPos + y,
                width,
                height
        ).build();
    }

    private void sendMenuButton(int id) {
        if (minecraft != null
                && minecraft.gameMode != null) {

            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    id
            );
        }
    }

    private void applyHeightInputs() {
        try {
            int minimum = Integer.parseInt(
                    minYInput.getValue()
            );

            int maximum = Integer.parseInt(
                    maxYInput.getValue()
            );

            if (minimum < -64
                    || maximum > 320
                    || minimum > maximum) {
                return;
            }

            int encoded = (minimum + 64) * 385
                    + (maximum + 64);

            sendMenuButton(3000 + encoded);

            minYInput.setFocused(false);
            maxYInput.setFocused(false);

        } catch (NumberFormatException ignored) {
            // Keine unvollständigen Zahlen senden.
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        updateButtonLabels();

        // Nicht während des Tippens überschreiben.
        if (minYInput != null
                && !minYInput.isFocused()) {
            String value = Integer.toString(menu.getMinY());

            if (!minYInput.getValue().equals(value)) {
                minYInput.setValue(value);
            }
        }

        if (maxYInput != null
                && !maxYInput.isFocused()) {
            String value = Integer.toString(menu.getMaxY());

            if (!maxYInput.getValue().equals(value)) {
                maxYInput.setValue(value);
            }
        }
    }

    private void updateButtonLabels() {
        if (startButton != null) {
            startButton.setMessage(
                    Component.literal(
                            menu.isRunning()
                                    ? "Stop"
                                    : "Start"
                    )
            );
        }

        if (silkButton != null) {
            silkButton.setMessage(
                    Component.literal(
                            menu.isSilkTouch()
                                    ? "Silk: AN"
                                    : "Silk: AUS"
                    )
            );
        }

        if (pushButton != null) {
            pushButton.setMessage(
                    Component.literal(
                            menu.isAutoPush()
                                    ? "Push: AN"
                                    : "Push: AUS"
                    )
            );
        }
    }

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

        // Dunkles Metallgehäuse.
        graphics.fill(
                x, y,
                x + GUI_WIDTH,
                y + GUI_HEIGHT,
                0xFF242D35
        );

        graphics.fill(
                x + 3, y + 3,
                x + GUI_WIDTH - 3,
                y + GUI_HEIGHT - 3,
                0xFF35434D
        );

        // Dunkler Steuerungsbereich.
        graphics.fill(
                x + 7, y + 18,
                x + 224, y + 103,
                0xFF18252B
        );

        drawEnergyBar(graphics, x, y);

        // 27 Lagerplätze.

        if (menu.isFilterPage()) {

            // Sechs Filterplätze anzeigen.
            for (int i = 0; i < 6; i++) {
                drawSlot(
                        graphics,
                        x + 22 + i * 22,
                        y + 118
                );
            }

        } else {

            // Normale 27 Lagerplätze.
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(
                            graphics,
                            x + 22 + col * 18,
                            y + 107 + row * 18
                    );
                }
            }

            // Vier Upgrade-Plätze.
            for (int i = 0; i < 4; i++) {
                drawSlot(
                        graphics,
                        x + 206,
                        y + 107 + i * 18
                );
            }
        }


        // Spielerinventar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(
                        graphics,
                        x + 22 + col * 18,
                        y + 180 + row * 18
                );
            }
        }

        // Hotbar.
        for (int col = 0; col < 9; col++) {
            drawSlot(
                    graphics,
                    x + 22 + col * 18,
                    y + 240
            );
        }
    }

    private void drawEnergyBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        int barX = x + ENERGY_X;
        int barY = y + ENERGY_Y;

        // Silberner Außenrahmen.
        graphics.fill(
                barX - 2,
                barY - 2,
                barX + ENERGY_W + 2,
                barY + ENERGY_H + 2,
                0xFFBFCED6
        );

        // Dunkler Hintergrund.
        graphics.fill(
                barX,
                barY,
                barX + ENERGY_W,
                barY + ENERGY_H,
                0xFF300C10
        );

        int energy = Math.clamp(
                menu.getEnergy(),
                0,
                GermaniumMinerBlockEntity.MAX_ENERGY
        );

        int filled = (int) (
                (energy / (double)
                        GermaniumMinerBlockEntity.MAX_ENERGY)
                        * ENERGY_H
        );

        if (filled > 0) {
            graphics.fill(
                    barX,
                    barY + ENERGY_H - filled,
                    barX + ENERGY_W,
                    barY + ENERGY_H,
                    0xFFE53935
            );
        }
    }

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        graphics.fill(
                x, y,
                x + 18, y + 18,
                0xFF718A91
        );

        graphics.fill(
                x + 1, y + 1,
                x + 17, y + 17,
                0xFF19252D
        );
    }

    @Override
    protected void extractLabels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.text(
                font,
                title,
                titleLabelX,
                titleLabelY,
                0xFF56DED2,
                false
        );

        graphics.text(
                font,
                Component.literal(
                        menu.isRunning()
                                ? "Status: Aktiv"
                                : "Status: Bereit"
                ),
                12,
                19,
                0xFF80F7D3,
                false
        );

        graphics.text(
                font,
                Component.literal("Min Y"),
                12,
                61,
                0xFFE6F2F5,
                false
        );

        graphics.text(
                font,
                Component.literal("Max Y"),
                111,
                61,
                0xFFE6F2F5,
                false
        );

        if (menu.isFilterPage()) {
            graphics.text(
                    font,
                    Component.literal("ItemStack-Filter (6)"),
                    23,
                    103,
                    0xFF56DED2,
                    false
            );

            graphics.text(
                    font,
                    Component.literal("Item anklicken = Filter setzen"),
                    23,
                    148,
                    0xFFE6F2F5,
                    false
            );

            graphics.text(
                    font,
                    Component.literal("Leere Hand = Filter entfernen"),
                    23,
                    160,
                    0xFFE6F2F5,
                    false
            );
        } else {
            graphics.text(
                    font,
                    Component.literal("Lager"),
                    23,
                    98,
                    0xFFE6F2F5,
                    false
            );

            graphics.text(
                    font,
                    Component.literal("UPG"),
                    204,
                    98,
                    0xFFE6F2F5,
                    false
            );
        }

        graphics.text(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
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

        int barX = leftPos + ENERGY_X;
        int barY = topPos + ENERGY_Y;

        if (mouseX >= barX - 2
                && mouseX < barX + ENERGY_W + 2
                && mouseY >= barY - 2
                && mouseY < barY + ENERGY_H + 2) {

            graphics.setTooltipForNextFrame(
                    Component.literal(
                            menu.getEnergy()
                                    + " / "
                                    + GermaniumMinerBlockEntity.MAX_ENERGY
                                    + " FE"
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
