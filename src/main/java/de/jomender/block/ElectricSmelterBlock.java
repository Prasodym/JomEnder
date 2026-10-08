
package de.jomender.block;

import de.jomender.ModBlockEntities;
import de.jomender.blockentity.ElectricSmelterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class ElectricSmelterBlock extends Block
        implements EntityBlock {

    public ElectricSmelterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ElectricSmelterBlockEntity(pos, state);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }

        if (type != ModBlockEntities.ELECTRIC_SMELTER.get()) {
            return null;
        }

        return (tickLevel, pos, blockState, blockEntity) -> {
            if (tickLevel instanceof ServerLevel serverLevel
                    && blockEntity instanceof ElectricSmelterBlockEntity smelter) {

                ElectricSmelterBlockEntity.serverTick(
                        serverLevel,
                        pos,
                        blockState,
                        smelter
                );
            }
        };
    }


    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return openSmelter(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        return openSmelter(level, pos, player);
    }

    private InteractionResult openSmelter(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.getBlockEntity(pos)
                instanceof ElectricSmelterBlockEntity smelter) {

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(smelter);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


}
