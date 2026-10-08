
package de.jomender.misc;

import de.jomender.ModBlocks;
import de.jomender.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class GalaxisConversionHandler {

    @SubscribeEvent
    public static void onRightClick(
            PlayerInteractEvent.RightClickBlock event
    ) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack heldItem = event.getItemStack();

        // Nur mit Germanium Clump
        if (!heldItem.is(ModItems.GermaniumClumb.get())) {
            return;
        }

        // Nur auf Vanilla Budding Amethyst
        if (!level.getBlockState(pos).is(Blocks.BUDDING_AMETHYST)) {
            return;
        }

        // Andere Rechtsklick-Aktionen verhindern
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        // Welt und Inventar nur auf dem Server verändern
        if (level.isClientSide()) {
            return;
        }

        // Vanilla-Block in Budding Galaxis umwandeln
        boolean converted = level.setBlock(
                pos,
                ModBlocks.BUDDING_GALAXIS.get().defaultBlockState(),
                3
        );

        // Nur bei erfolgreicher Umwandlung verbrauchen
        if (converted && !event.getEntity().getAbilities().instabuild) {
            heldItem.shrink(1);
        }
    }
}
