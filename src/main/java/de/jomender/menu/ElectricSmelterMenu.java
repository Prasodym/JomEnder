
package de.jomender.menu;

import de.jomender.ModMenuTypes;
import de.jomender.blockentity.ElectricSmelterBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ElectricSmelterMenu extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData data;

    // Client-Konstruktor
    public ElectricSmelterMenu(int id, Inventory playerInventory) {
        this(
                id,
                playerInventory,
                new SimpleContainer(
                        ElectricSmelterBlockEntity.TOTAL_SLOTS
                ),
                new SimpleContainerData(3)
        );
    }

    // Server-Konstruktor
    public ElectricSmelterMenu(
            int id,
            Inventory playerInventory,
            Container inventory,
            ContainerData data
    ) {
        super(ModMenuTypes.ELECTRIC_SMELTER.get(), id);

        this.inventory = inventory;
        this.data = data;

        checkContainerSize(
                inventory, ElectricSmelterBlockEntity.TOTAL_SLOTS
        );
        checkContainerDataCount(data, 3);
        addDataSlots(data);

        // 3 Input-Slots: 0-2
        for (int i = 0; i < 3; i++) {
            addSlot(new Slot(
                    inventory, i, 25 + i * 22, 37
            ));
        }

        // 6 Output-Slots: 3-8
        for (int i = 0; i < 6; i++) {
            int x = 129 + (i % 3) * 22;
            int y = 28 + (i / 3) * 22;

            addSlot(new Slot(inventory, i + 3, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // 4 Upgrade-Slots: 9-12
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(
                    inventory, i + 9, 77 + i * 22, 86
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Spielerinventar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        35 + col * 18,
                        117 + row * 18
                ));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(
                    playerInventory, col,
                    35 + col * 18, 175
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-Klick später ergänzen
        return ItemStack.EMPTY;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getProgress() {
        return data.get(1);
    }

    public int getMaxProgress() {
        return data.get(2);
    }
}
