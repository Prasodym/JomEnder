package de.jomender.client.screen;

import de.jomender.client.gui.JomEnderGuiLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.reactor.ReactorControllerBlockEntity;
import de.jomender.reactor.ReactorControllerBlockEntity.Status;
import de.jomender.reactor.ReactorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** GUI des Reaktor-Controllers. */
public class ReactorScreen extends JomEnderMachineScreen<ReactorMenu> {
    private static final int HEIGHT = 190;

    // Balken: Hitze, Wasser, Energie (alle 10x64)
    private static final int BAR_Y = 22, BAR_W = 10, BAR_H = 64;
    private static final int HEAT_X = 123, WATER_X = 137, ENERGY_X = 151;

    // Text-Bereich zwischen Slots und Balken
    private static final int INFO_X = 50, INFO_Y = 22, INFO_W = 70;

    // Brennstoff-Vorrat als kleiner Balken unter dem Uran-Slot
    private static final int FUEL_BAR_X = ReactorMenu.FUEL_X - 1, FUEL_BAR_Y = ReactorMenu.FUEL_Y + 19;

    private static final int HEAT_COLOR = 0xFFFF7A2E;
    private static final int HEAT_HOT_COLOR = 0xFFFF3030;
    private static final int WATER_COLOR = 0xFF3C7DFF;
    private static final int FUEL_COLOR = 0xFF7CFC00;
    private static final int OK_COLOR = JomEnderGuiTheme.TITLE_COLOR;
    private static final int WARN_COLOR = 0xFFFFC23D;
    private static final int BAD_COLOR = 0xFFFF5555;

    private Button toggleButton;

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, JomEnderGuiLayout.WIDTH, HEIGHT);
        titleLabelX = 10;
        titleLabelY = 7;
        inventoryLabelX = 9;
        inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        toggleButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ReactorMenu.BUTTON_TOGGLE);
            }
        }).bounds(leftPos + INFO_X, topPos + 72, 50, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        toggleButton.setMessage(Component.translatable(menu.isActive()
                ? "gui.jomender.reactor.button.on" : "gui.jomender.reactor.button.off"));
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        JomEnderGuiTheme.drawPanel(g, leftPos, topPos, JomEnderGuiLayout.PANEL_WIDTH, HEIGHT);

        JomEnderGuiTheme.drawSlot(g, leftPos + ReactorMenu.FUEL_X - 1, topPos + ReactorMenu.FUEL_Y - 1);
        JomEnderGuiTheme.drawSlot(g, leftPos + ReactorMenu.WASTE_X - 1, topPos + ReactorMenu.WASTE_Y - 1);
        drawHorizontalBar(g, leftPos + FUEL_BAR_X, topPos + FUEL_BAR_Y, 18, 4,
                menu.getFuel(), ReactorControllerBlockEntity.FUEL_PER_INGOT, FUEL_COLOR);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JomEnderGuiTheme.drawSlot(g, leftPos + ReactorMenu.PLAYER_X + col * 18 - 1,
                        topPos + ReactorMenu.PLAYER_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            JomEnderGuiTheme.drawSlot(g, leftPos + ReactorMenu.PLAYER_X + col * 18 - 1, topPos + ReactorMenu.HOTBAR_Y - 1);
        }

        int heat = menu.getHeat();
        drawVerticalBar(g, leftPos + HEAT_X, topPos + BAR_Y, heat, ReactorControllerBlockEntity.MAX_HEAT,
                heat > ReactorControllerBlockEntity.MAX_HEAT * 7 / 10 ? HEAT_HOT_COLOR : HEAT_COLOR);
        drawVerticalBar(g, leftPos + WATER_X, topPos + BAR_Y, menu.getWater(),
                ReactorControllerBlockEntity.WATER_CAPACITY, WATER_COLOR);
        JomEnderGuiTheme.verticalEnergy(g, leftPos + ENERGY_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergy(), ReactorControllerBlockEntity.ENERGY_CAPACITY);

        drawJomEnderPopups(g);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, JomEnderGuiTheme.TITLE_COLOR, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, JomEnderGuiTheme.TEXT_COLOR, false);

        Status status = menu.getStatus();
        g.text(font, Component.translatable("gui.jomender.reactor.status." + status.name().toLowerCase()),
                INFO_X, INFO_Y, statusColor(status), false);

        if (status == Status.NOT_FORMED) {
            // Grund in mehreren Zeilen
            List<FormattedCharSequence> lines =
                    font.split(Component.translatable(menu.getProblem().translationKey()), INFO_W);
            int y = INFO_Y + 12;
            for (int i = 0; i < Math.min(3, lines.size()); i++, y += 10) {
                g.text(font, lines.get(i), INFO_X, y, JomEnderGuiTheme.TEXT_COLOR, false);
            }
            BlockPos where = menu.getProblemPos();
            if (where != null) {
                g.text(font, where.getX() + " " + where.getY() + " " + where.getZ(),
                        INFO_X, y, WARN_COLOR, false);
            }
            return;
        }

        int[] size = menu.getSize();
        if (size != null) {
            g.text(font, size[0] + "×" + size[1] + "×" + size[2], INFO_X, INFO_Y + 12,
                    JomEnderGuiTheme.TEXT_COLOR, false);
        }
        g.text(font, String.format("%,d FE/t", menu.getFePerTick()), INFO_X, INFO_Y + 24,
                JomEnderGuiTheme.TEXT_COLOR, false);
        g.text(font, (menu.getHeat() / 10) + " °C", INFO_X, INFO_Y + 36,
                menu.getHeat() > ReactorControllerBlockEntity.MAX_HEAT * 7 / 10 ? BAD_COLOR : JomEnderGuiTheme.TEXT_COLOR,
                false);
    }

    private static int statusColor(Status status) {
        return switch (status) {
            case RUNNING -> OK_COLOR;
            case NOT_FORMED, SCRAM -> BAD_COLOR;
            case OFF -> JomEnderGuiTheme.TEXT_COLOR;
            default -> WARN_COLOR;
        };
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        if (isOver(mouseX, mouseY, HEAT_X, BAR_Y, BAR_W, BAR_H)) {
            g.setTooltipForNextFrame(Component.translatable("gui.jomender.reactor.heat",
                    menu.getHeat() / 10, ReactorControllerBlockEntity.MAX_HEAT / 10), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, WATER_X, BAR_Y, BAR_W, BAR_H)) {
            g.setTooltipForNextFrame(Component.translatable("gui.jomender.reactor.water",
                    String.format("%,d", menu.getWater()),
                    String.format("%,d", ReactorControllerBlockEntity.WATER_CAPACITY)), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, ENERGY_X, BAR_Y, BAR_W, BAR_H)) {
            g.setTooltipForNextFrame(Component.literal(String.format("%,d / %,d FE",
                    menu.getEnergy(), ReactorControllerBlockEntity.ENERGY_CAPACITY)), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, FUEL_BAR_X, FUEL_BAR_Y, 18, 4)) {
            g.setTooltipForNextFrame(Component.translatable("gui.jomender.reactor.fuel",
                    menu.getFuel() * 100 / ReactorControllerBlockEntity.FUEL_PER_INGOT), mouseX, mouseY);
        }
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private static void drawVerticalBar(GuiGraphicsExtractor g, int x, int y, int amount, int max, int color) {
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF6D6974);
        g.fill(x + 1, y + 1, x + BAR_W - 1, y + BAR_H - 1, 0xFF171722);
        int inner = BAR_H - 4;
        int fill = max <= 0 ? 0 : (int) ((long) Math.clamp(amount, 0, max) * inner / max);
        if (fill > 0) g.fill(x + 2, y + BAR_H - 2 - fill, x + BAR_W - 2, y + BAR_H - 2, color);
    }

    private static void drawHorizontalBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int amount, int max, int color) {
        g.fill(x, y, x + w, y + h, 0xFF6D6974);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF171722);
        int fill = max <= 0 ? 0 : (int) ((long) Math.clamp(amount, 0, max) * (w - 2) / max);
        if (fill > 0) g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
    }
}
