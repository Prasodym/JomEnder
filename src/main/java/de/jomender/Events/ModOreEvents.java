package de.jomender.Events;

import de.jomender.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class ModOreEvents {

    public static void onBreak(BreakBlockEvent event) {

        var player = event.getPlayer();
        var state = event.getState();

        boolean germaniumOre =
                state.is(ModBlocks.GERMANIUM_ORE.get()) ||
                        state.is(ModBlocks.DEEPSLATE_GERMANIUM_ORE.get()) ||
                        state.is(ModBlocks.NETHER_GERMANIUM_ORE.get()) ||
                        state.is(ModBlocks.END_GERMANIUM_ORE.get());

        if (!germaniumOre) return;

        // Kreativmodus ausnehmen
        if (player.isCreative()) return;

        // Netherite-Spitzhacke ist erlaubt
        if (player.getMainHandItem().is(Items.NETHERITE_PICKAXE)) {
            return;
        }

        // Normales Abbauen verhindern
        event.setCanceled(true);

        // Explosion nur auf dem Server auslösen
        if (player.level() instanceof ServerLevel level) {

            var pos = event.getPos();

            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    2.0F,
                    Level.ExplosionInteraction.BLOCK
            );
        }
    }
}
