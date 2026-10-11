package de.jomender.client.screen;

import de.jomender.blockentity.ArcFusionChamberBlockEntity;
import de.jomender.client.gui.ArcFusionLayout;
import de.jomender.client.gui.JomEnderGuiTheme;
import de.jomender.menu.ArcFusionChamberMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

public class ArcFusionChamberScreen extends JomEnderMachineScreen<ArcFusionChamberMenu> {

    public ArcFusionChamberScreen(ArcFusionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ArcFusionLayout.WIDTH, ArcFusionLayout.HEIGHT);
        enableUpgrades(true);
        enableDirectionConfig(true);
        titleLabelX = 10;
        titleLabelY = 7;
        inventoryLabelX = 9;
        inventoryLabelY = ArcFusionLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics,
                                  int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        JomEnderGuiTheme.drawPanel(graphics, leftPos, topPos,
                ArcFusionLayout.PANEL_WIDTH, ArcFusionLayout.HEIGHT);

        for (int i = 0; i < 2; i++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + ArcFusionLayout.INPUT_X + i * ArcFusionLayout.SLOT_GAP - 1,
                    topPos + ArcFusionLayout.INPUT_Y - 1);
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + ArcFusionLayout.OUTPUT_X + i * ArcFusionLayout.SLOT_GAP - 1,
                    topPos + ArcFusionLayout.OUTPUT_Y - 1);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JomEnderGuiTheme.drawSlot(graphics,
                        leftPos + ArcFusionLayout.PLAYER_X + col * 18 - 1,
                        topPos + ArcFusionLayout.PLAYER_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            JomEnderGuiTheme.drawSlot(graphics,
                    leftPos + ArcFusionLayout.PLAYER_X + col * 18 - 1,
                    topPos + ArcFusionLayout.HOTBAR_Y - 1);
        }

        JomEnderGuiTheme.downwardProgress(graphics,
                leftPos + ArcFusionLayout.PROGRESS_X, topPos + ArcFusionLayout.PROGRESS_Y,
                menu.getProgress(), menu.getMaxProgress());

        JomEnderGuiTheme.verticalEnergy(graphics,
                leftPos + ArcFusionLayout.ENERGY_X, topPos + ArcFusionLayout.ENERGY_Y,
                ArcFusionLayout.ENERGY_W, ArcFusionLayout.ENERGY_H,
                menu.getEnergy(), ArcFusionChamberBlockEntity.MAX_ENERGY);

        drawJomEnderPopups(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, JomEnderGuiTheme.TITLE_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                JomEnderGuiTheme.TEXT_COLOR, false);

        boolean fusing = menu.getProgress() > 0;
        int x = ArcFusionLayout.STATUS_X;
        int y = ArcFusionLayout.STATUS_Y;
        graphics.text(font, Component.literal(fusing ? "Fusing" : "Idle"), x, y,
                fusing ? JomEnderGuiTheme.TITLE_COLOR : JomEnderGuiTheme.TEXT_COLOR, false);
        if (fusing) {
            int percent = menu.getMaxProgress() <= 0 ? 0 : menu.getProgress() * 100 / menu.getMaxProgress();
            graphics.text(font, Component.literal(percent + "%"), x, y + 12, JomEnderGuiTheme.TEXT_COLOR, false);
            graphics.text(font, Component.literal(menu.getEnergyPerTick() + " FE/t"), x, y + 24,
                    JomEnderGuiTheme.TEXT_COLOR, false);
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
        int x = leftPos + ArcFusionLayout.ENERGY_X;
        int y = topPos + ArcFusionLayout.ENERGY_Y;
        if (mouseX >= x && mouseX < x + ArcFusionLayout.ENERGY_W
                && mouseY >= y && mouseY < y + ArcFusionLayout.ENERGY_H) {
            graphics.setTooltipForNextFrame(Component.literal(
                    menu.getEnergy() + " / " + ArcFusionChamberBlockEntity.MAX_ENERGY + " FE"
                            + " (energy only from below)"),
                    mouseX, mouseY);
        }
    }
}
