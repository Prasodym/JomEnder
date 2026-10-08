package de.jomender.misc;

import de.jomender.ModBlocks;
import de.jomender.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntitySpawnReason;

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

        // Nur mit Germanium Clumb
        if (!heldItem.is(ModItems.GermaniumClumb.get())) {
            return;
        }

        // Nur auf Vanilla Budding Amethyst
        if (!level.getBlockState(pos).is(Blocks.BUDDING_AMETHYST)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (level.isClientSide()) {
            return;
        }

        boolean converted = level.setBlock(
                pos,
                ModBlocks.BUDDING_GALAXIS.get().defaultBlockState(),
                3
        );


        if (converted) {

            if (level instanceof ServerLevel serverLevel) {
                LightningBolt lightning =
                        EntityType.LIGHTNING_BOLT.create(
                                serverLevel,
                                EntitySpawnReason.TRIGGERED
                        );

                if (lightning != null) {
                    lightning.setPos(
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5
                    );

                    // Nur optischer Blitz: kein Feuer, kein Schaden
                    lightning.setVisualOnly(true);

                    serverLevel.addFreshEntity(lightning);
                }
            }

            if (!event.getEntity().getAbilities().instabuild) {
                heldItem.shrink(1);
            }
        }

    }
}