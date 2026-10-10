package de.jomender.client.screen;

import de.jomender.blockentity.ElectricSmelterBlockEntity;
import de.jomender.client.gui.JomEnderGuiLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.menu.ElectricSmelterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

public class ElectricSmelterScreen extends JomEnderMachineScreen<ElectricSmelterMenu> {
    public ElectricSmelterScreen(ElectricSmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, JomEnderGuiLayout.WIDTH, JomEnderGuiLayout.HEIGHT);
        enableUpgrades(true);
        enableDirectionConfig(true);
        titleLabelX = 10;
        titleLabelY = 7;
        // The artwork already includes the player inventory.
        inventoryLabelX = 9;
        inventoryLabelY = 183;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics,
                                  int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        JomEnderGuiTheme.drawPanel(graphics, leftPos, topPos,
                JomEnderGuiLayout.PANEL_WIDTH, JomEnderGuiLayout.HEIGHT);
        // Slot borders from MCGUI Crafter. The actual slots are positioned by the Menu.
        for (int i = 0; i < ElectricSmelterBlockEntity.INPUT_COUNT; i++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + JomEnderGuiLayout.INPUT_X + i * JomEnderGuiLayout.INPUT_GAP - 1,
                    topPos + JomEnderGuiLayout.INPUT_Y - 1);
        }
        for (int i = 0; i < 8; i++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + JomEnderGuiLayout.OUTPUT_X + (i % 4) * JomEnderGuiLayout.OUTPUT_GAP - 1,
                    topPos + JomEnderGuiLayout.OUTPUT_Y + (i / 4) * JomEnderGuiLayout.OUTPUT_GAP - 1);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JomEnderGuiTheme.drawSlot(graphics,
                        leftPos + JomEnderGuiLayout.PLAYER_X + col * 18 - 1,
                        topPos + JomEnderGuiLayout.PLAYER_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + JomEnderGuiLayout.PLAYER_X + col * 18 - 1,
                    topPos + JomEnderGuiLayout.HOTBAR_Y - 1);
        }
        JomEnderGuiTheme.downwardProgress(graphics,
                leftPos + JomEnderGuiLayout.PROGRESS_X,
                topPos + JomEnderGuiLayout.PROGRESS_Y,
                menu.getProgress(), menu.getMaxProgress());
        JomEnderGuiTheme.verticalEnergy(graphics,
                leftPos + JomEnderGuiLayout.ENERGY_X,
                topPos + JomEnderGuiLayout.ENERGY_Y,
                JomEnderGuiLayout.ENERGY_W, JomEnderGuiLayout.ENERGY_H,
                menu.getEnergy(), ElectricSmelterBlockEntity.MAX_ENERGY);
        drawJomEnderPopups(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, JomEnderGuiTheme.TITLE_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                JomEnderGuiTheme.TEXT_COLOR, false);

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
        int x = leftPos + JomEnderGuiLayout.ENERGY_X;
        int y = topPos + JomEnderGuiLayout.ENERGY_Y;
        if (mouseX >= x && mouseX < x + JomEnderGuiLayout.ENERGY_W
                && mouseY >= y && mouseY < y + JomEnderGuiLayout.ENERGY_H) {
            graphics.setTooltipForNextFrame(Component.literal(
                    menu.getEnergy() + " / " + ElectricSmelterBlockEntity.MAX_ENERGY + " FE"),
                    mouseX, mouseY);
        }
    }
}
