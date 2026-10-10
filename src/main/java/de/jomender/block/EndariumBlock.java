package de.jomender.block;

import de.jomender.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EndariumBlock extends Block {

    public EndariumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5, 0.25, 0.25, 0.25, 0.01);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // Gelegentliche Mini-Blitze an einer zufälligen Blockoberfläche.
        if (random.nextInt(4) != 0) {
            return;
        }

        int side = random.nextInt(6);
        double x = pos.getX() + 0.10 + random.nextDouble() * 0.80;
        double y = pos.getY() + 0.10 + random.nextDouble() * 0.80;
        double z = pos.getZ() + 0.10 + random.nextDouble() * 0.80;
        double outside = 0.04;

        switch (side) {
            case 0 -> y = pos.getY() - outside;
            case 1 -> y = pos.getY() + 1 + outside;
            case 2 -> z = pos.getZ() - outside;
            case 3 -> z = pos.getZ() + 1 + outside;
            case 4 -> x = pos.getX() - outside;
            case 5 -> x = pos.getX() + 1 + outside;
            default -> { }
        }

        level.addParticle(ModParticles.ENDARIUM_LIGHTNING.get(), x, y, z,
                0.0, 0.0, 0.0);

        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                    0.0, 0.015, 0.0);
        }
    }
}
