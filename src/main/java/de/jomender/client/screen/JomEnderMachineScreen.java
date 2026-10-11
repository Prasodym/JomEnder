package de.jomender.client.screen;

import de.jomender.client.gui.JomEnderGuiLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.sideconfig.RelativeSide;
import de.jomender.sideconfig.SideConfigMenu;
import de.jomender.sideconfig.SideMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Shared machine frame, U/D controls and movable popups. */
public abstract class JomEnderMachineScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    private static final int POPUP_WIDTH = 128;
    private static final int POPUP_HEIGHT = 53;

    // Seitenkonfiguration: 6 Kacheln als Kreuz + Legende rechts daneben
    private static final int SIDE_POPUP_WIDTH = 140;
    private static final int SIDE_POPUP_HEIGHT = 98;
    private static final int TILE = 20;
    private static final int TILE_STEP = 22;
    private static final int GRID_X = 8, GRID_Y = 26;
    private static final int LEGEND_X = 82, LEGEND_Y = 30;
    private static final int TILE_BG = 0xFF171722;
    private static final int TILE_BG_HOVER = 0xFF2A2A38;
    private static final int TILE_LOCKED = 0xFF2E2E36;

    private int mouseXCache, mouseYCache;
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
                // Das Seiten-Popup ist größer: sicherstellen, dass es auf dem Bildschirm bleibt.
                movePopupTo(getPopupX(), getPopupY());
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

    private boolean sideConfigOpen() {
        return directionPopupOpen && menu instanceof SideConfigMenu;
    }

    private int popupWidth() { return sideConfigOpen() ? SIDE_POPUP_WIDTH : POPUP_WIDTH; }
    private int popupHeight() { return sideConfigOpen() ? SIDE_POPUP_HEIGHT : POPUP_HEIGHT; }

    private void updateCloseButtons() {
        if (upgradeClose != null) upgradeClose.visible = upgradePopupOpen;
        if (directionClose != null) directionClose.visible = directionPopupOpen;
        int cx = getPopupX() + popupWidth() - CLOSE_SIZE - 5;
        int cy = getPopupY() + 5;
        if (upgradeClose != null) { upgradeClose.setX(cx); upgradeClose.setY(cy); }
        if (directionClose != null) { directionClose.setX(cx); directionClose.setY(cy); }
    }

    /** Clamp popup inside the visible Minecraft screen, not just the machine panel. */
    private void movePopupTo(int absoluteX, int absoluteY) {
        int x = Math.clamp(absoluteX, 0, Math.max(0, width - popupWidth()));
        int y = Math.clamp(absoluteY, 0, Math.max(0, height - popupHeight()));
        if (x == getPopupX() && y == getPopupY()) return;
        popupRelativeX = x - leftPos;
        popupRelativeY = y - topPos;
        onUpgradePopupMoved(popupRelativeX, popupRelativeY);
        updateCloseButtons();
    }

    private boolean insidePopup(double mx, double my) {
        return popupOpen() && mx >= getPopupX() && mx < getPopupX() + popupWidth()
                && my >= getPopupY() && my < getPopupY() + popupHeight();
    }

    private boolean onCloseButton(double mx, double my) {
        int x = getPopupX() + popupWidth() - CLOSE_SIZE - 5;
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
        // Klick auf eine Seitenkachel: links = nächster Modus, rechts = vorheriger.
        if (sideConfigOpen() && (event.button() == 0 || event.button() == 1)) {
            RelativeSide hit = sideTileAt(event.x(), event.y());
            if (hit != null) {
                Direction side = hit.toAbsolute(machineFacing());
                if (minecraft.gameMode != null && ((SideConfigMenu) menu).isSideConfigurable(side)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                            SideConfigMenu.buttonId(side, event.button() == 0));
                }
                return true;
            }
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
        JomEnderGuiTheme.drawPanel(graphics, x, y, popupWidth(), popupHeight());
        graphics.text(font, Component.literal(upgradePopupOpen ? "UPGRADES" : "SIDES"),
                x + 7, y + 8, JomEnderGuiTheme.TITLE_COLOR, false);
        if (sideConfigOpen()) {
            drawSideConfig(graphics, x, y);
        } else if (directionPopupOpen) {
            graphics.text(font, Component.literal("Seitenkonfiguration folgt"),
                    x + 7, y + 28, JomEnderGuiTheme.TEXT_COLOR, false);
        } else {
            for (int i = 0; i < 4; i++) {
                JomEnderGuiTheme.drawSlot(graphics,
                        x + 17 + i * JomEnderGuiLayout.UPGRADE_GAP, y + 26);
            }
        }
    }
    // =========================
    // SEITENKONFIGURATION
    // =========================

    /** Rasterposition (Spalte, Zeile) jeder Kachel im Kreuz-Layout. */
    private static int[] tileCell(RelativeSide side) {
        return switch (side) {
            case TOP -> new int[]{1, 0};
            case LEFT -> new int[]{0, 1};
            case FRONT -> new int[]{1, 1};
            case RIGHT -> new int[]{2, 1};
            case BOTTOM -> new int[]{1, 2};
            case BACK -> new int[]{2, 2};
        };
    }

    private int tileX(RelativeSide side) { return getPopupX() + GRID_X + tileCell(side)[0] * TILE_STEP; }
    private int tileY(RelativeSide side) { return getPopupY() + GRID_Y + tileCell(side)[1] * TILE_STEP; }

    private RelativeSide sideTileAt(double mx, double my) {
        if (!sideConfigOpen()) return null;
        for (RelativeSide side : RelativeSide.values()) {
            int tx = tileX(side), ty = tileY(side);
            if (mx >= tx && mx < tx + TILE && my >= ty && my < ty + TILE) return side;
        }
        return null;
    }

    private Direction machineFacing() {
        if (minecraft.level == null || !(menu instanceof SideConfigMenu sc)) return Direction.NORTH;
        BlockState state = minecraft.level.getBlockState(sc.getMachinePos());
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    private BlockState neighborState(Direction side) {
        if (minecraft.level == null || !(menu instanceof SideConfigMenu sc)) return null;
        BlockPos pos = sc.getMachinePos().relative(side);
        BlockState state = minecraft.level.getBlockState(pos);
        return state.isAir() ? null : state;
    }

    private void drawSideConfig(GuiGraphicsExtractor g, int x, int y) {
        SideConfigMenu sc = (SideConfigMenu) menu;
        Direction facing = machineFacing();
        RelativeSide hovered = sideTileAt(mouseXCache, mouseYCache);

        for (RelativeSide side : RelativeSide.values()) {
            Direction abs = side.toAbsolute(facing);
            SideMode mode = sc.getSideMode(abs);
            boolean locked = !sc.isSideConfigurable(abs);
            int tx = tileX(side), ty = tileY(side);

            // Farbiger Rahmen = Modus (gesperrte Seiten: dunkelgrau)
            g.fill(tx, ty, tx + TILE, ty + TILE, locked ? TILE_LOCKED : mode.color());
            g.fill(tx + 2, ty + 2, tx + TILE - 2, ty + TILE - 2,
                    side == hovered && !locked ? TILE_BG_HOVER : TILE_BG);

            // Nachbarblock als Item, sonst der Seitenbuchstabe
            BlockState neighbor = neighborState(abs);
            ItemStack stack = neighbor == null ? ItemStack.EMPTY : new ItemStack(neighbor.getBlock().asItem());
            if (!stack.isEmpty()) {
                g.item(stack, tx + 2, ty + 2);
            } else {
                String letter = side.shortName();
                g.text(font, letter, tx + (TILE - font.width(letter)) / 2 + 1, ty + 6,
                        JomEnderGuiTheme.TEXT_COLOR, false);
            }
        }

        // Legende
        SideMode[] modes = SideMode.values();
        for (int i = 0; i < modes.length; i++) {
            int lx = x + LEGEND_X;
            int ly = y + LEGEND_Y + i * 13;
            g.fill(lx, ly, lx + 8, ly + 8, modes[i].color());
            g.text(font, modes[i].label(), lx + 12, ly, JomEnderGuiTheme.TEXT_COLOR, false);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        mouseXCache = mouseX;
        mouseYCache = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        RelativeSide hovered = sideTileAt(mouseX, mouseY);
        if (hovered != null) {
            Direction abs = hovered.toAbsolute(machineFacing());
            SideConfigMenu sc = (SideConfigMenu) menu;
            SideMode mode = sc.getSideMode(abs);
            boolean locked = !sc.isSideConfigurable(abs);
            Component note = sc.getSideNote(abs);
            BlockState neighbor = neighborState(abs);

            List<Component> lines = new ArrayList<>();
            lines.add(hovered.displayName().copy()
                    .append(Component.literal(" (" + capitalize(abs.getName()) + ")")
                            .withStyle(ChatFormatting.GRAY)));
            if (locked) {
                lines.add(Component.literal("Locked").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.literal("Mode: ").append(mode.label().withColor(mode.color() & 0xFFFFFF)));
            }
            if (note != null) {
                lines.add(note.copy().withStyle(ChatFormatting.AQUA));
            }
            lines.add(neighbor == null
                    ? Component.literal("No neighbor").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal("Neighbor: ").append(neighbor.getBlock().getName())
                            .withStyle(ChatFormatting.GRAY));
            if (!locked) {
                lines.add(Component.literal("Left/Right-click: change mode").withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
