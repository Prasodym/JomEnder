package de.jomender.upgrade;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Energiespeicher, dessen Annahme/Abgabe pro Übertragung zur Laufzeit angepasst werden kann
 * (z.B. wenn Speed-Upgrades mehr FE/t brauchen).
 */
public class MachineEnergyHandler extends SimpleEnergyHandler {

    private final Runnable onChanged;

    public MachineEnergyHandler(int capacity, int maxInsert, int maxExtract, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.onChanged = onChanged;
    }

    public void setMaxInsert(int value) {
        this.maxInsert = Math.clamp(value, 0, capacity);
    }

    public void setMaxExtract(int value) {
        this.maxExtract = Math.clamp(value, 0, capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
