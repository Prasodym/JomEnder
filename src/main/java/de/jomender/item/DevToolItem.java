package de.jomender.item;

import de.jomender.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Entwickler-Werkzeug.
 * Rechtsklick auf einen Spawner: Spawner erzeugt ab jetzt Galaxius.
 * Shift + Rechtsklick auf einen Block: ein Galaxius erscheint direkt dort (zum Testen).
 */
public class DevToolItem extends Item {

    public DevToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpawnerBlockEntity spawner && (player == null || !player.isShiftKeyDown())) {
            spawner.setEntityId(ModEntities.GALAXIUS.get(), level.getRandom());
            spawner.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
            message(player, Component.translatable("message.jomender.dev_tool.spawner")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }

        if (player != null && player.isShiftKeyDown()) {
            BlockPos spawnPos = pos.relative(context.getClickedFace());
            if (ModEntities.GALAXIUS.get().spawn(serverLevel, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE) != null) {
                message(player, Component.translatable("message.jomender.dev_tool.spawned")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void message(Player player, Component text) {
        if (player != null) player.sendOverlayMessage(text);
    }
}
