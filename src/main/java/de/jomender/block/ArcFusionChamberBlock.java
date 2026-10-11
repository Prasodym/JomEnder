package de.jomender.block;

import de.jomender.ModBlockEntities;
import de.jomender.blockentity.ArcFusionChamberBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Arc Fusion Chamber: verschmilzt zwei Items zu einem (Rezepte als JSON). */
public class ArcFusionChamberBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ArcFusionChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Vorderseite (Modell-Nordseite) zeigt beim Platzieren zum Spieler, wie beim Ofen.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // =========================
    // BLOCK ENTITY
    // =========================

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcFusionChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ARC_FUSION_CHAMBER.get()) {
            return null;
        }
        if (level.isClientSide()) {
            // Client: Animation und Partikel
            return (tickLevel, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof ArcFusionChamberBlockEntity chamber) {
                    ArcFusionChamberBlockEntity.clientTick(tickLevel, pos, blockState, chamber);
                }
            };
        }
        return (tickLevel, pos, blockState, blockEntity) -> {
            if (tickLevel instanceof ServerLevel serverLevel
                    && blockEntity instanceof ArcFusionChamberBlockEntity chamber) {
                ArcFusionChamberBlockEntity.serverTick(serverLevel, pos, blockState, chamber);
            }
        };
    }

    // =========================
    // GUI ÖFFNEN
    // =========================

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof ArcFusionChamberBlockEntity chamber) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(chamber, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
