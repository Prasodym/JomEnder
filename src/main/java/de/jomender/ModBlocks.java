package de.jomender;

import de.jomender.block.BuddingGalaxisBlock;
import de.jomender.block.ElectricSmelterBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import de.jomender.block.CoalGeneratorBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Jomender.MOD_ID);


    public static final DeferredBlock<DropExperienceBlock> GERMANIUM_ORE =
            BLOCKS.registerBlock("germanium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(3), props),
                    props -> props
                            .mapColor(MapColor.STONE)
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_GERMANIUM_ORE =
            BLOCKS.registerBlock("deepslate_germanium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(4), props),
                    props -> props
                            .mapColor(MapColor.DEEPSLATE)
                            .strength(0.5f, 3.0f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<DropExperienceBlock> NETHER_GERMANIUM_ORE =
            BLOCKS.registerBlock("nether_germanium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(5), props),
                    props -> props
                            .mapColor(MapColor.NETHER)
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.NETHER_ORE)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<DropExperienceBlock> END_GERMANIUM_ORE =
            BLOCKS.registerBlock("end_germanium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(5), props),
                    props -> props
                            .mapColor(MapColor.SAND)
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );


    public static final DeferredBlock<Block> COAL_GENERATOR =
            BLOCKS.registerBlock(
                    "coal_generator",
                    CoalGeneratorBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.5f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<Block> ELECTRIC_SMELTER =
            BLOCKS.registerBlock(
                    "electric_smelter",
                    ElectricSmelterBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.5f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            );



    public static final DeferredBlock<BuddingGalaxisBlock> BUDDING_GALAXIS =
            BLOCKS.registerBlock(
                    "budding_galaxis",
                    BuddingGalaxisBlock::new,
                    properties -> properties
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.AMETHYST)
            );

    public static final DeferredBlock<AmethystClusterBlock>
            SMALL_GALAXIS_BUD = BLOCKS.registerBlock(
            "small_galaxis_bud",
            properties -> new AmethystClusterBlock(
                    3, 4, properties
            ),
            properties -> properties
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
    );

    public static final DeferredBlock<AmethystClusterBlock>
            MEDIUM_GALAXIS_BUD = BLOCKS.registerBlock(
            "medium_galaxis_bud",
            properties -> new AmethystClusterBlock(
                    4, 3, properties
            ),
            properties -> properties
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
    );

    public static final DeferredBlock<AmethystClusterBlock>
            LARGE_GALAXIS_BUD = BLOCKS.registerBlock(
            "large_galaxis_bud",
            properties -> new AmethystClusterBlock(
                    5, 3, properties
            ),
            properties -> properties
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
    );

    public static final DeferredBlock<AmethystClusterBlock>
            GALAXIS_CLUSTER = BLOCKS.registerBlock(
            "galaxis_cluster",
            properties -> new AmethystClusterBlock(
                    7, 3, properties
            ),
            properties -> properties
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
    );


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
