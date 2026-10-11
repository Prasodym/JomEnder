package de.jomender.sideconfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;

/**
 * Für Menüs, deren Maschine eine Seitenkonfiguration hat.
 * Der Screen schickt Klicks über den Vanilla-Mechanismus clickMenuButton.
 */
public interface SideConfigMenu {

    /** Button-IDs: BUTTON_FORWARD + Richtung bzw. BUTTON_BACKWARD + Richtung. */
    int BUTTON_FORWARD = 100;
    int BUTTON_BACKWARD = 110;

    BlockPos getMachinePos();

    /** Datenslot, der die gepackte Seitenkonfiguration enthält. */
    ContainerData getSideData();

    int getSideDataIndex();

    /** Darf diese Seite im GUI umgestellt werden? Gesperrte Seiten werden grau angezeigt. */
    default boolean isSideConfigurable(Direction side) {
        return true;
    }

    /** Optionaler Hinweis im Tooltip einer Seite, z.B. "Energy input". */
    default Component getSideNote(Direction side) {
        return null;
    }

    default SideMode getSideMode(Direction side) {
        return SideConfig.read(getSideData().get(getSideDataIndex()), side);
    }

    static int buttonId(Direction side, boolean forward) {
        return (forward ? BUTTON_FORWARD : BUTTON_BACKWARD) + side.get3DDataValue();
    }

    /** Serverseitig aus clickMenuButton aufrufen. Gibt true zurück, wenn der Klick behandelt wurde. */
    default boolean handleSideButton(int buttonId) {
        boolean forward;
        int dir;
        if (buttonId >= BUTTON_FORWARD && buttonId < BUTTON_FORWARD + 6) {
            forward = true;
            dir = buttonId - BUTTON_FORWARD;
        } else if (buttonId >= BUTTON_BACKWARD && buttonId < BUTTON_BACKWARD + 6) {
            forward = false;
            dir = buttonId - BUTTON_BACKWARD;
        } else {
            return false;
        }
        Direction side = Direction.from3DDataValue(dir);
        if (!isSideConfigurable(side)) {
            return false;
        }
        ContainerData data = getSideData();
        int index = getSideDataIndex();
        data.set(index, SideConfig.cycle(data.get(index), side, forward));
        return true;
    }
}
