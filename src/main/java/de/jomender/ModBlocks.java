package de.jomender;

import de.jomender.block.*;
import de.jomender.reactor.ReactorCasingBlock;
import de.jomender.reactor.ReactorControllerBlock;
import de.jomender.reactor.ReactorGlassBlock;
import de.jomender.reactor.ReactorPortBlock;
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

    // Uran: nur in der Deepslate-Schicht, so hart wie Vanilla-Deepslate-Erze
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_URANIUM_ORE =
            BLOCKS.registerBlock("deepslate_uranium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(4), props),
                    props -> props
                            .mapColor(MapColor.DEEPSLATE)
                            .strength(4.5f, 3.0f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<Block> URANIUM_BLOCK =
            BLOCKS.registerSimpleBlock("uranium_block",
                    props -> props
                            .mapColor(MapColor.COLOR_LIGHT_GREEN)
                            .strength(5.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<Block> RAW_URANIUM_BLOCK =
            BLOCKS.registerSimpleBlock("raw_uranium_block",
                    props -> props
                            .mapColor(MapColor.COLOR_GREEN)
                            .strength(5.0f, 6.0f)
                            .sound(SoundType.STONE)
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

    public static final DeferredBlock<DropExperienceBlock> Endarium_Ore =
            BLOCKS.registerBlock("endarium_ore",
                    props -> new DropExperienceBlock(ConstantInt.of(5), props),
                    props -> props
                            .mapColor(MapColor.SAND)
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );
    public static final DeferredBlock<DropExperienceBlock> Endarium_Raw_Block =
            BLOCKS.registerBlock("endarium_raw_block",
                    props -> new DropExperienceBlock(ConstantInt.of(5), props),
                    props -> props
                            .mapColor(MapColor.SAND)
                            .strength(3.0f, 3.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );
    public static final DeferredBlock<Block> ENDARIUM_BLOCK =
            BLOCKS.registerBlock(
                    "endarium_block",
                    EndariumBlock::new,
                    props -> props
                            .strength(5.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 8)
            );
    public static final DeferredBlock<Block> Germanium_Raw_Block =
            BLOCKS.registerBlock(
                    "germanium_raw_block",
                    Block::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            );
    public static final DeferredBlock<Block> Germanium_of_Block =
            BLOCKS.registerBlock(
                    "germanium_of_block",
                    Block::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 6.0f)
                            .sound(SoundType.METAL)
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
                            .noOcclusion()
            );

    public static final DeferredBlock<Block> ARC_FUSION_CHAMBER =
            BLOCKS.registerBlock(
                    "arc_fusion_chamber",
                    ArcFusionChamberBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.5f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
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
                            .noOcclusion()
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


    public static final DeferredBlock<Block> GERMANIUM_MINER =
            BLOCKS.registerBlock(
                    "germanium_miner",
                    GermaniumMinerBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(4.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            );

    public static final DeferredBlock<Block> GERMANIUM_MINER_PART =
            BLOCKS.registerBlock(
                    "germanium_miner_part",
                    GermaniumMinerPartBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(4.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            );



    // =========================
    // REAKTOR (Multiblock 3x3x3 bis 7x7x7, siehe reactor/ReactorStructure)
    // =========================

    public static final DeferredBlock<ReactorCasingBlock> REACTOR_CASING =
            BLOCKS.registerBlock("reactor_casing", ReactorCasingBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL));

    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER =
            BLOCKS.registerBlock("reactor_controller", ReactorControllerBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL));

    public static final DeferredBlock<ReactorPortBlock> REACTOR_PORT =
            BLOCKS.registerBlock("reactor_port", ReactorPortBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL));

    /** Nur in Wandflächen erlaubt. Durchsichtig - man sieht das Kühlwasser. */
    public static final DeferredBlock<ReactorGlassBlock> REACTOR_GLASS =
            BLOCKS.registerBlock("reactor_glass", ReactorGlassBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .isValidSpawn((state, level, pos, entity) -> false)
                            .isRedstoneConductor((state, level, pos) -> false)
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false));

    /** Unendliche Wasserquelle für Pipes aller Mods (kein Transfer-Limit). */
    public static final DeferredBlock<InfiniteWaterBlock> INFINITE_WATER =
            BLOCKS.registerBlock("infinite_water", InfiniteWaterBlock::new,
                    props -> props
                            .mapColor(MapColor.WATER)
                            .strength(3.5f, 6.0f)
                            .sound(SoundType.METAL));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
