
package de.jomender.block;

import de.jomender.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import de.jomender.ModBlockEntities;
import de.jomender.blockentity.GermaniumMinerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

public class GermaniumMinerBlock extends Block implements EntityBlock {

    public GermaniumMinerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GermaniumMinerBlockEntity(pos, state);
    }
    // Der Controller steht unten in der Mitte.
    // X und Z: -1 bis +1
    // Y: 0 bis +2


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();

        // Sicherstellen, dass alle 27 Positionen
        // innerhalb der erlaubten Bauhöhe liegen.
        if (origin.getY() + 2 >= level.getMaxY()) {
            return null;
        }

        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {

                    BlockPos target = origin.offset(x, y, z);

                    if (!level.hasChunkAt(target)) {
                        return null;
                    }

                    // Der Controller-Block selbst wird
                    // bereits durch BlockItem geprüft.
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockState existing = level.getBlockState(target);

                    // Nichts Bestehendes zerstören.
                    // Für Version 1: nur Luft erlauben.
                    if (!existing.isAir()) {
                        return null;
                    }
                }
            }
        }

        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide()) {
            return;
        }

        // Die übrigen 26 Strukturblöcke erzeugen.
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos target = pos.offset(x, y, z);

                    BlockState part =
                            ModBlocks.GERMANIUM_MINER_PART.get()
                                    .defaultBlockState()
                                    .setValue(
                                            GermaniumMinerPartBlock.OFFSET_X,
                                            x + 1
                                    )
                                    .setValue(
                                            GermaniumMinerPartBlock.OFFSET_Y,
                                            y
                                    )
                                    .setValue(
                                            GermaniumMinerPartBlock.OFFSET_Z,
                                            z + 1
                                    );

                    level.setBlock(target, part, 3);
                }
            }
        }
    }
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                instanceof GermaniumMinerBlockEntity miner) {
            serverPlayer.openMenu(miner);
        }

        return InteractionResult.SUCCESS;
    }

    // Wird nach Entfernung des Controllers aufgerufen.
    // Alle zugehörigen Strukturteile verschwinden.
    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(
                state, level, pos, movedByPiston
        );

        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos target = pos.offset(x, y, z);
                    BlockState part = level.getBlockState(target);

                    if (!part.is(ModBlocks.GERMANIUM_MINER_PART.get())) {
                        continue;
                    }

                    // Nur Teile dieser Maschine entfernen.
                    if (part.getValue(GermaniumMinerPartBlock.OFFSET_X) != x + 1
                            || part.getValue(GermaniumMinerPartBlock.OFFSET_Y) != y
                            || part.getValue(GermaniumMinerPartBlock.OFFSET_Z) != z + 1) {
                        continue;
                    }

                    level.removeBlock(target, false);
                }
            }
        }
    }
}
