
package de.jomender.menu;

import de.jomender.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CoalGeneratorMenu extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData data;

    // Client: Platzhalter für 5 Slots und 3 Datenwerte
    public CoalGeneratorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(5),
                new SimpleContainerData(3));
    }

    // Optionaler Zwischenkonstruktor
    public CoalGeneratorMenu(
            int id,
            Inventory playerInventory,
            Container inventory
    ) {
        this(id, playerInventory,
                inventory,
                new SimpleContainerData(3));
    }

    // Server: echtes Inventar und echte Energiedaten
    public CoalGeneratorMenu(
            int id,
            Inventory playerInventory,
            Container inventory,
            ContainerData data
    ) {
        super(ModMenuTypes.COAL_GENERATOR.get(), id);

        this.inventory = inventory;
        this.data = data;

        checkContainerSize(inventory, 5);
        checkContainerDataCount(data, 3);

        this.addDataSlots(data);

        // Slot 0: Brennstoff
        this.addSlot(new Slot(inventory, 0, 23, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            }
        });

        // Slots 1-4: Upgrade-Slots
        for (int i = 0; i < 4; i++) {
            int x = 119 + (i % 2) * 22;
            int y = 25 + (i / 2) * 22;

            this.addSlot(new Slot(inventory, i + 1, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // Später für echte Upgrade-Items freischalten
                    return false;
                }
            });
        }

        // Spielerinventar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-Klick bauen wir später sauber ein
        return ItemStack.EMPTY;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getBurnTime() {
        return data.get(1);
    }

    public int getMaxBurnTime() {
        return data.get(2);
    }
}