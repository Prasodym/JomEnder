package de.jomender.client.screen;

import de.jomender.client.gui.JomEnderGuiLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.NonNull;

/** Shared machine frame, U/D controls and movable popups. */
public abstract class JomEnderMachineScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    private static final int POPUP_WIDTH = 128;
    private static final int POPUP_HEIGHT = 53;
    private static final int HEADER_HEIGHT = 22;
    private static final int CLOSE_SIZE = 16;

    private boolean upgradesEnabled;
    private boolean directionEnabled;
    private boolean upgradePopupOpen;
    private boolean directionPopupOpen;
    private boolean draggingPopup;

    // Coordinates relative to the main GUI origin, shared by both popup types.
    private int popupRelativeX = 36;
    private int popupRelativeY = 135;
    private double dragOffsetX;
    private double dragOffsetY;

    private Button upgradeClose;
    private Button directionClose;

    protected JomEnderMachineScreen(T menu, Inventory inventory, Component title,
                                    int width, int height) {
        super(menu, inventory, title, width, height);
    }

    protected final void enableUpgrades(boolean enabled) { upgradesEnabled = enabled; }
    protected final void enableDirectionConfig(boolean enabled) { directionEnabled = enabled; }
    protected final boolean isUpgradePopupOpen() { return upgradePopupOpen; }

    /** Called whenever the popup is opened or closed. */
    protected void onUpgradePopupChanged(boolean open) {}

    /** Called when popup position changes. x/y are relative to the menu origin. */
    protected void onUpgradePopupMoved(int x, int y) {}

    protected final int getPopupX() { return leftPos + popupRelativeX; }
    protected final int getPopupY() { return topPos + popupRelativeY; }

    protected int machineButtonX() { return JomEnderGuiLayout.BUTTON_X; }

    @Override
    protected void init() {
        super.init();
        draggingPopup = false;
        upgradePopupOpen = false;
        directionPopupOpen = false;

        // Preserve popup position between screen resizes; ensure it remains on-screen.
        movePopupTo(getPopupX(), getPopupY());
        onUpgradePopupChanged(false);

        if (upgradesEnabled) {
            addRenderableWidget(Button.builder(Component.literal("U"), button -> {
                upgradePopupOpen = !upgradePopupOpen;
                directionPopupOpen = false;
                draggingPopup = false;
                onUpgradePopupMoved(popupRelativeX, popupRelativeY);
                onUpgradePopupChanged(upgradePopupOpen);
                updateCloseButtons();
            }).bounds(leftPos + machineButtonX(),
                    topPos + JomEnderGuiLayout.BUTTON_U_Y, 20, 20).build());

            upgradeClose = addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                upgradePopupOpen = false;
                draggingPopup = false;
                onUpgradePopupChanged(false);
                updateCloseButtons();
            }).bounds(getPopupX() + POPUP_WIDTH - CLOSE_SIZE - 5,
                    getPopupY() + 5, CLOSE_SIZE, CLOSE_SIZE).build());
        }

        if (directionEnabled) {
            addRenderableWidget(Button.builder(Component.literal("D"), button -> {
                directionPopupOpen = !directionPopupOpen;
                upgradePopupOpen = false;
                draggingPopup = false;
                onUpgradePopupChanged(false);
                updateCloseButtons();
            }).bounds(leftPos + machineButtonX(),
                    topPos + JomEnderGuiLayout.BUTTON_D_Y, 20, 20).build());

            directionClose = addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                directionPopupOpen = false;
                draggingPopup = false;
                updateCloseButtons();
            }).bounds(getPopupX() + POPUP_WIDTH - CLOSE_SIZE - 5,
                    getPopupY() + 5, CLOSE_SIZE, CLOSE_SIZE).build());
        }
        updateCloseButtons();
    }

    private boolean popupOpen() { return upgradePopupOpen || directionPopupOpen; }

    private void updateCloseButtons() {
        if (upgradeClose != null) upgradeClose.visible = upgradePopupOpen;
        if (directionClose != null) directionClose.visible = directionPopupOpen;
        int cx = getPopupX() + POPUP_WIDTH - CLOSE_SIZE - 5;
        int cy = getPopupY() + 5;
        if (upgradeClose != null) { upgradeClose.setX(cx); upgradeClose.setY(cy); }
        if (directionClose != null) { directionClose.setX(cx); directionClose.setY(cy); }
    }

    /** Clamp popup inside the visible Minecraft screen, not just the machine panel. */
    private void movePopupTo(int absoluteX, int absoluteY) {
        int x = Math.clamp(absoluteX, 0, Math.max(0, width - POPUP_WIDTH));
        int y = Math.clamp(absoluteY, 0, Math.max(0, height - POPUP_HEIGHT));
        if (x == getPopupX() && y == getPopupY()) return;
        popupRelativeX = x - leftPos;
        popupRelativeY = y - topPos;
        onUpgradePopupMoved(popupRelativeX, popupRelativeY);
        updateCloseButtons();
    }

    private boolean insidePopup(double mx, double my) {
        return popupOpen() && mx >= getPopupX() && mx < getPopupX() + POPUP_WIDTH
                && my >= getPopupY() && my < getPopupY() + POPUP_HEIGHT;
    }

    private boolean onCloseButton(double mx, double my) {
        int x = getPopupX() + POPUP_WIDTH - CLOSE_SIZE - 5;
        int y = getPopupY() + 5;
        return mx >= x && mx < x + CLOSE_SIZE && my >= y && my < y + CLOSE_SIZE;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && popupOpen()
                && insidePopup(event.x(), event.y())
                && event.y() < getPopupY() + HEADER_HEIGHT
                && !onCloseButton(event.x(), event.y())) {
            draggingPopup = true;
            dragOffsetX = event.x() - getPopupX();
            dragOffsetY = event.y() - getPopupY();
            return true;
        }
        // The popup intercepts clicks that are not on its own slots or close button.
        if (insidePopup(event.x(), event.y()) && !onCloseButton(event.x(), event.y())) {
            if (!isOverPopupUpgradeSlot(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverPopupUpgradeSlot(double mouseX, double mouseY) {
        if (!upgradePopupOpen) return false;
        for (int i = 0; i < 4; i++) {
            int sx = getPopupX() + 18 + i * JomEnderGuiLayout.UPGRADE_GAP;
            int sy = getPopupY() + 27;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPopup) {
            movePopupTo((int)Math.round(event.x() - dragOffsetX),
                    (int)Math.round(event.y() - dragOffsetY));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPopup && event.button() == 0) {
            draggingPopup = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Draws the shared 9-slice popup background and slot frames. */
    protected final void drawJomEnderPopups(@NonNull GuiGraphicsExtractor graphics) {
        if (!popupOpen()) return;
        int x = getPopupX();
        int y = getPopupY();
        JomEnderGuiTheme.drawPanel(graphics, x, y, POPUP_WIDTH, POPUP_HEIGHT);
        graphics.text(font, Component.literal(upgradePopupOpen ? "UPGRADES" : "DIRECTION"),
                x + 7, y + 8, JomEnderGuiTheme.TITLE_COLOR, false);
        if (directionPopupOpen) {
            graphics.text(font, Component.literal("Seitenkonfiguration folgt"),
                    x + 7, y + 28, JomEnderGuiTheme.TEXT_COLOR, false);
        } else {
            for (int i = 0; i < 4; i++) {
                JomEnderGuiTheme.drawSlot(graphics,
                        x + 17 + i * JomEnderGuiLayout.UPGRADE_GAP, y + 26);
            }
        }
    }
}
