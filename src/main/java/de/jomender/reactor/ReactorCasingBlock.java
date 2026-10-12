package de.jomender.reactor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Reaktor-Gehäuse. Kann sich optisch mit dem restlichen Reaktor verbinden. */
public class ReactorCasingBlock extends Block {
    public ReactorCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(ReactorParts.defaults(stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        ReactorParts.addProperties(builder);
    }
}
