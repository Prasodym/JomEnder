
package de.jomender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ModEnergyTransfer {

    private ModEnergyTransfer() {
    }

    /**
     * Verteilt insgesamt maximal maxTransfer FE
     * an direkt benachbarte Energieempfänger.
     */
    public static void sendToNeighbors(
            Level level,
            BlockPos sourcePos,
            EnergyHandler source,
            int maxTransfer
    ) {
        sendToNeighbors(level, sourcePos, source, maxTransfer, side -> true);
    }

    /**
     * Wie oben, aber nur über Seiten, die allowedSide erlaubt
     * (z.B. Seiten mit Output in der Seitenkonfiguration).
     */
    public static void sendToNeighbors(
            Level level,
            BlockPos sourcePos,
            EnergyHandler source,
            int maxTransfer,
            Predicate<Direction> allowedSide
    ) {
        if (level.isClientSide() || maxTransfer <= 0) {
            return;
        }

        int remaining = Math.min(
                maxTransfer,
                source.getAmountAsInt()
        );

        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                break;
            }

            if (!allowedSide.test(direction)) {
                continue;
            }

            BlockPos targetPos = sourcePos.relative(direction);

            if (!level.isLoaded(targetPos)) {
                continue;
            }

            EnergyHandler target = level.getCapability(
                    Capabilities.Energy.BLOCK,
                    targetPos,
                    direction.getOpposite()
            );

            if (target == null || target == source) {
                continue;
            }

            // Beide Änderungen gehören zu EINER Transaktion.
            try (Transaction transaction = Transaction.openRoot()) {

                int inserted = target.insert(
                        remaining,
                        transaction
                );

                if (inserted <= 0) {
                    continue;
                }

                int extracted = source.extract(
                        inserted,
                        transaction
                );

                // Nur vollständig passende Transfers bestätigen.
                // Andernfalls rollt die Transaktion zurück.
                if (extracted != inserted) {
                    continue;
                }

                transaction.commit();
                remaining -= inserted;
            }
        }
    }
}
