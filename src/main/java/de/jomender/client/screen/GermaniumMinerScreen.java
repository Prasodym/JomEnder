package de.jomender.client.screen;

import de.jomender.blockentity.GermaniumMinerBlockEntity;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.menu.GermaniumMinerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

/** First miner layout pass: MCGUI coordinates, shared panels and live start/stop. */
public class GermaniumMinerScreen extends JomEnderMachineScreen<GermaniumMinerMenu> {
    // MCGUI background (-11,-110) is the local origin (0,0).
    // Main panel 202x280; U/D live to its right.
    private static final int PANEL_WIDTH = 202;
    private static final int GUI_WIDTH = 224;
    private static final int GUI_HEIGHT = 280;
    private static final int START_X = 115, START_Y = 25, ACTION_W = 51, ACTION_H = 17;
    private static final int CONFIG_X = 115, CONFIG_Y = 46;
    private static final int PREVIEW_X = 115, PREVIEW_Y = 105;
    private static final int SILK_X = 67, PUSH_X = 41, TOGGLE_Y = 80;
    private static final int TOGGLE_W = 20, TOGGLE_H = 10;
    private static final int ENERGY_X = 170, ENERGY_Y = 25, ENERGY_W = 10, ENERGY_H = 99;

    private EditBox minYInput;
    private EditBox maxYInput;

    public GermaniumMinerScreen(GermaniumMinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        enableUpgrades(true);
        enableDirectionConfig(true);
        titleLabelX = 47;
        titleLabelY = 7;
        inventoryLabelX = 19;
        inventoryLabelY = 180;
    }

    @Override
    protected void init() {
        super.init();
        // Keep the existing height controls until the Config popup is expanded.
        minYInput = new EditBox(font, leftPos + 25, topPos + 56, 31, 12,
                Component.literal("Min Y"));
        maxYInput = new EditBox(font, leftPos + 61, topPos + 56, 31, 12,
                Component.literal("Max Y"));
        configureHeightInput(minYInput, menu.getMinY());
        configureHeightInput(maxYInput, menu.getMaxY());
        addRenderableWidget(minYInput);
        addRenderableWidget(maxYInput);
        addRenderableWidget(Button.builder(Component.literal("OK"), b -> applyHeightInputs())
                .bounds(leftPos + 95, topPos + 56, 18, 12).build());
        onUpgradePopupChanged(false);
    }

    private void configureHeightInput(EditBox box, int value) {
        box.setMaxLength(4);
        box.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{1,3}"));
        box.setValue(Integer.toString(value));
    }

    private void sendMenuButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void applyHeightInputs() {
        try {
            int minimum = Integer.parseInt(minYInput.getValue());
            int maximum = Integer.parseInt(maxYInput.getValue());
            if (minimum < -64 || maximum > 320 || minimum > maximum) return;
            sendMenuButton(3000 + (minimum + 64) * 385 + maximum + 64);
            minYInput.setFocused(false);
            maxYInput.setFocused(false);
        } catch (NumberFormatException ignored) { }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (minYInput != null && !minYInput.isFocused()) {
            String value = Integer.toString(menu.getMinY());
            if (!minYInput.getValue().equals(value)) minYInput.setValue(value);
        }
        if (maxYInput != null && !maxYInput.isFocused()) {
            String value = Integer.toString(menu.getMaxY());
            if (!maxYInput.getValue().equals(value)) maxYInput.setValue(value);
        }
    }

    private boolean hovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w
                && my >= topPos + y && my < topPos + y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (hovering(event.x(), event.y(), START_X, START_Y, ACTION_W, ACTION_H)) {
                sendMenuButton(GermaniumMinerMenu.BUTTON_START);
                return true;
            }
            if (hovering(event.x(), event.y(), CONFIG_X, CONFIG_Y, ACTION_W, ACTION_H)) {
                menu.setFilterPage(!menu.isFilterPage());
                sendMenuButton(GermaniumMinerMenu.BUTTON_FILTER_PAGE);
                return true;
            }
            if (hovering(event.x(), event.y(), PREVIEW_X, PREVIEW_Y, ACTION_W, ACTION_H)) {
                sendMenuButton(GermaniumMinerMenu.BUTTON_PREVIEW);
                return true;
            }
            if (hovering(event.x(), event.y(), SILK_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
                sendMenuButton(GermaniumMinerMenu.BUTTON_SILK);
                return true;
            }
            if (hovering(event.x(), event.y(), PUSH_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
                sendMenuButton(GermaniumMinerMenu.BUTTON_PUSH);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int x = leftPos, y = topPos;
        JomEnderGuiTheme.drawPanel(g, x, y, PANEL_WIDTH, GUI_HEIGHT);
        // Left control panel. From user's original backgound_miner.png.
        drawMinerBackground(g, x + 21, y + 25, 92, 99);

        // 27 storage slots (or ghost filter slots on alternate page).
        if (menu.isFilterPage()) {
            for (int i = 0; i < GermaniumMinerBlockEntity.FILTER_SLOTS; i++) {
                JomEnderGuiTheme.drawSlot(g, x + 19 + i * 18, y + 128);
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    JomEnderGuiTheme.drawSlot(g, x + 19 + col * 18, y + 128 + row * 18);
                }
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JomEnderGuiTheme.drawSlot(g, x + 19 + col * 18, y + 194 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++)
            JomEnderGuiTheme.drawSlot(g, x + 19 + col * 18, y + 252);

        // Buttons reflect server-synced miner state; Hover only when not pressed.
        boolean running = menu.isRunning();
        drawTexturedAction(g, START_X, START_Y, running ? "Stop" : "Start",
                running, hovering(mouseX, mouseY, START_X, START_Y, ACTION_W, ACTION_H));
        drawTexturedAction(g, CONFIG_X, CONFIG_Y, "Filter",
                menu.isFilterPage(), hovering(mouseX, mouseY, CONFIG_X, CONFIG_Y, ACTION_W, ACTION_H));
        drawTexturedAction(g, PREVIEW_X, PREVIEW_Y,
                menu.isPreviewEnabled() ? "Preview ON" : "Preview OFF",
                menu.isPreviewEnabled(),
                hovering(mouseX, mouseY, PREVIEW_X, PREVIEW_Y, ACTION_W, ACTION_H));
        drawToggle(g, SILK_X, TOGGLE_Y, menu.isSilkTouch(), "S");
        drawToggle(g, PUSH_X, TOGGLE_Y, menu.isAutoPush(), "P");

        JomEnderGuiTheme.verticalEnergy(g, x + ENERGY_X, y + ENERGY_Y,
                ENERGY_W, ENERGY_H, menu.getEnergy(), GermaniumMinerBlockEntity.MAX_ENERGY);
        drawJomEnderPopups(g);
    }

    private void drawMinerBackground(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        // Dark distinct zone; transparent PNG could replace this later.
        g.fill(x, y, x + width, y + height, 0xFF111113);
        g.fill(x, y, x + width, y + 1, 0xFF282831);
    }

    private void drawTexturedAction(GuiGraphicsExtractor g, int x, int y,
                                    String caption, boolean pressed, boolean hover) {
        JomEnderGuiTheme.drawMachineButton(g, leftPos + x, topPos + y,
                ACTION_W, ACTION_H, pressed, hover);
        int w = font.width(caption);
        g.text(font, Component.literal(caption), leftPos + x + (ACTION_W - w) / 2,
                topPos + y + 4, 0xFFE5E6E8, true);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean enabled, String text) {
        JomEnderGuiTheme.drawMachineButton(g, leftPos + x, topPos + y,
                TOGGLE_W, TOGGLE_H, enabled,
                hovering(lastMouseX, lastMouseY, x, y, TOGGLE_W, TOGGLE_H));
        g.text(font, Component.literal(text), leftPos + x + 6, topPos + y + 1,
                enabled ? 0xFF4FF2A0 : 0xFFE6E6E8, false);
    }

    private int lastMouseX, lastMouseY;

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Screen-relative titles; background-phase buttons are drawn in absolute coordinates.
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        g.text(font, title, titleLabelX, titleLabelY, JomEnderGuiTheme.TITLE_COLOR, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                JomEnderGuiTheme.TEXT_COLOR, false);
        g.text(font, Component.literal(menu.isRunning() ? "Running" : "Idle"),
                26, 32, menu.isRunning() ? 0xFF31DC88 : 0xFFE55757, false);
        // This value and mined block preview aren't present in the current 6 menu data slots.
        g.text(font, Component.literal("To Mine: --"), 26, 43, 0xFFB7B7C0, false);
        g.text(font, Component.literal("Y:"), 26, 49, 0xFFB7B7C0, false);
        if (menu.isFilterPage()) {
            g.text(font, Component.literal("Filter: Item auf die sechs Slots klicken"),
                    19, 113, 0xFF5CDC9D, false);
        }
    }

    @Override
    protected void onUpgradePopupChanged(boolean open) {
        menu.setUpgradePopupOpen(open);
    }

    @Override
    protected void onUpgradePopupMoved(int relativeX, int relativeY) {
        menu.moveUpgradeSlots(relativeX, relativeY);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g,
                                   int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (hovering(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H)) {
            g.setTooltipForNextFrame(Component.literal(menu.getEnergy() + " / "
                    + GermaniumMinerBlockEntity.MAX_ENERGY + " FE"), mouseX, mouseY);
        }
    }
}
