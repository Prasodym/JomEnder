package de.jomender.building;

import de.jomender.ModBlocks;
import de.jomender.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Bausteine: je Sorte ein Block, eine Stufe und eine Treppe - nur zum Bauen.
 * Block und Stufe haben verbundene Texturen (siehe {@link ConnectedBlock}).
 *
 * Neue Sorte: unten eine Zeile mit {@link #family} ergänzen und Texturen/Modelle
 * nach dem Muster der vorhandenen anlegen.
 */
public final class BuildingBlocks {
    private BuildingBlocks() {}

    /** Eine Sorte Bausteine. */
    public record Family(String name, DeferredBlock<ConnectedBlock> block, DeferredBlock<ConnectedSlabBlock> slab,
                         DeferredBlock<StairBlock> stairs, DeferredItem<BlockItem> blockItem,
                         DeferredItem<BlockItem> slabItem, DeferredItem<BlockItem> stairsItem) {}

    public static final List<Family> ALL = new ArrayList<>();

    public static final Family MACHINE_PLATING = family("machine_plating",
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final Family GERMANIUM_PLATING = family("germanium_plating",
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final Family ENDARIUM_TILES = family("endarium_tiles",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 9.0F).sound(SoundType.DEEPSLATE_TILES)
                    .requiresCorrectToolForDrops().lightLevel(state -> 4));

    public static final Family GALAXIS_BRICKS = family("galaxis_bricks",
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.0F, 6.0F).sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops());

    private static Family family(String name, UnaryOperator<BlockBehaviour.Properties> props) {
        DeferredBlock<ConnectedBlock> block = ModBlocks.BLOCKS.registerBlock(name,
                p -> new ConnectedBlock(name, p), props);
        DeferredBlock<ConnectedSlabBlock> slab = ModBlocks.BLOCKS.registerBlock(name + "_slab",
                p -> new ConnectedSlabBlock(name, p), props);
        DeferredBlock<StairBlock> stairs = ModBlocks.BLOCKS.registerBlock(name + "_stairs",
                p -> new StairBlock(block.get().defaultBlockState(), p), props);
        Family family = new Family(name, block, slab, stairs,
                ModItems.ITEMS.registerSimpleBlockItem(name, block),
                ModItems.ITEMS.registerSimpleBlockItem(name + "_slab", slab),
                ModItems.ITEMS.registerSimpleBlockItem(name + "_stairs", stairs));
        ALL.add(family);
        return family;
    }

    /** Lädt die Klasse, damit sich alle Blöcke rechtzeitig eintragen (aus dem Mod-Konstruktor aufrufen). */
    public static void init() {}
}
