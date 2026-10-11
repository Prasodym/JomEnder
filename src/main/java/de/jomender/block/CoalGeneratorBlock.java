
package de.jomender.block;

import de.jomender.ModBlockEntities;
import de.jomender.blockentity.CoalGeneratorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class CoalGeneratorBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    // Konstruktor
    public CoalGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.SOUTH)
        );
    }

    // Speichert die Ausrichtung im BlockState
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Generator schaut beim Platzieren zum Spieler
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection()
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new CoalGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }

        if (type != ModBlockEntities.COAL_GENERATOR.get()) {
            return null;
        }

        return (tickLevel, pos, blockState, blockEntity) -> {
            CoalGeneratorBlockEntity.serverTick(
                    tickLevel,
                    pos,
                    blockState,
                    (CoalGeneratorBlockEntity) blockEntity
            );
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
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            if (level.getBlockEntity(pos)
                    instanceof CoalGeneratorBlockEntity generator) {
                serverPlayer.openMenu(generator, buf -> buf.writeBlockPos(pos));
            }
        }

        return InteractionResult.SUCCESS;
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
                && player instanceof ServerPlayer serverPlayer) {

            if (level.getBlockEntity(pos)
                    instanceof CoalGeneratorBlockEntity generator) {
                serverPlayer.openMenu(generator, buf -> buf.writeBlockPos(pos));
            }
        }

        return InteractionResult.SUCCESS;
    }
}
