package de.jomender.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Modus pro Weltrichtung. Wird für den GUI-Sync in ein einziges int gepackt
 * (2 Bit pro Seite, 12 Bit insgesamt - passt auch in einen Short-Datenslot).
 */
public final class SideConfig {

    private static final int BITS = 2;
    private static final int MASK = (1 << BITS) - 1;

    private final SideMode[] modes = new SideMode[6];
    private final Runnable onChanged;

    /**
     * @param defaults  Standardmodus pro Richtung (Index = Direction#get3DDataValue)
     * @param onChanged wird aufgerufen, wenn sich ein Modus ändert
     */
    public SideConfig(SideMode[] defaults, Runnable onChanged) {
        System.arraycopy(defaults, 0, modes, 0, 6);
        this.onChanged = onChanged;
    }

    public static SideMode[] defaults(SideMode all) {
        SideMode[] d = new SideMode[6];
        java.util.Arrays.fill(d, all);
        return d;
    }

    public SideMode get(Direction side) {
        return modes[side.get3DDataValue()];
    }

    public void set(Direction side, SideMode mode) {
        int i = side.get3DDataValue();
        if (modes[i] == mode) return;
        modes[i] = mode;
        onChanged.run();
    }

    public int pack() {
        int packed = 0;
        for (int i = 0; i < 6; i++) {
            packed |= modes[i].ordinal() << (i * BITS);
        }
        return packed;
    }

    public void unpack(int packed) {
        boolean changed = false;
        for (int i = 0; i < 6; i++) {
            SideMode mode = SideMode.byId((packed >> (i * BITS)) & MASK);
            if (modes[i] != mode) {
                modes[i] = mode;
                changed = true;
            }
        }
        if (changed) onChanged.run();
    }

    /** Liest einen Modus aus einem gepackten Wert (für die Client-Seite). */
    public static SideMode read(int packed, Direction side) {
        return SideMode.byId((packed >> (side.get3DDataValue() * BITS)) & MASK);
    }

    /** Liefert einen neuen gepackten Wert, in dem eine Seite weitergeschaltet wurde. */
    public static int cycle(int packed, Direction side, boolean forward) {
        int shift = side.get3DDataValue() * BITS;
        SideMode current = SideMode.byId((packed >> shift) & MASK);
        SideMode next = forward ? current.next() : current.previous();
        return (packed & ~(MASK << shift)) | (next.ordinal() << shift);
    }

    public void save(ValueOutput output, String key) {
        output.putInt(key, pack());
    }

    /** Lädt gespeicherte Werte; fehlt der Eintrag (alte Welt), bleiben die Standardwerte. */
    public void load(ValueInput input, String key) {
        int packed = input.getIntOr(key, -1);
        if (packed >= 0) {
            for (int i = 0; i < 6; i++) {
                modes[i] = SideMode.byId((packed >> (i * BITS)) & MASK);
            }
        }
    }
}
