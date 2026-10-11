package de.jomender;

import de.jomender.item.EndariumTools;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Jomender.MOD_ID);

    public static final DeferredItem<Item> Germanium =
            ITEMS.registerSimpleItem("germanium_ingot");

    public static final DeferredItem<Item> Efficient_upgrade =
            ITEMS.registerSimpleItem("efficient_upgrade");

    public static final DeferredItem<Item> Speed_upgrade =
            ITEMS.registerSimpleItem("speed_upgrade");

    public static final DeferredItem<Item> Case =
            ITEMS.registerSimpleItem("case");

    public static final DeferredItem<Item> MachineFrame =
            ITEMS.registerSimpleItem("machine_frame");

    public static final DeferredItem<Item> MachineCore =
            ITEMS.registerSimpleItem("machine_core");

    public static final DeferredItem<Item> MachineCircuit =
            ITEMS.registerSimpleItem("machine_circuit");

    public static final DeferredItem<Item> MachineMotor =
            ITEMS.registerSimpleItem("machine_motor");

    public static final DeferredItem<Item> Magnetic =
            ITEMS.registerSimpleItem("magnetic");

    // =========================
    // ENDARIUM-WERKZEUG
    // =========================

    /** Über Netherite, unzerstörbar, feuerfest. Nur per Schmiedetisch aus der Netherite-Spitzhacke. */
    public static final DeferredItem<Item> ENDARIUM_PICKAXE =
            ITEMS.registerItem("endarium_pickaxe", Item::new, props -> props
                    .pickaxe(EndariumTools.ENDARIUM, 1.0F, -2.8F)
                    .fireResistant()
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                    .rarity(Rarity.EPIC));

    public static final DeferredItem<SmithingTemplateItem> ENDARIUM_UPGRADE_SMITHING_TEMPLATE =
            ITEMS.registerItem("endarium_upgrade_smithing_template",
                    EndariumTools::createUpgradeTemplate,
                    props -> props.rarity(Rarity.EPIC));

    public static final DeferredItem<Item> Raw_Uranium =
            ITEMS.registerSimpleItem("raw_uranium");

    public static final DeferredItem<Item> Uranium_Ingot =
            ITEMS.registerSimpleItem("uranium_ingot");

    public static final DeferredItem<Item> Uranium_Dust =
            ITEMS.registerSimpleItem("uranium_dust");

    public static final DeferredItem<BlockItem> URANIUM_BLOCK =
            ITEMS.registerSimpleBlockItem("uranium_block", ModBlocks.URANIUM_BLOCK);

    public static final DeferredItem<BlockItem> RAW_URANIUM_BLOCK =
            ITEMS.registerSimpleBlockItem("raw_uranium_block", ModBlocks.RAW_URANIUM_BLOCK);

    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_uranium_ore", ModBlocks.DEEPSLATE_URANIUM_ORE);

    public static final DeferredItem<Item> GermaniumClump =
            ITEMS.registerSimpleItem("germanium_clumb");

    public static final DeferredItem<BlockItem> Germanium_Raw_Block =
            ITEMS.registerSimpleBlockItem("germanium_raw_block", ModBlocks.Germanium_Raw_Block);

    // Endarium Ore

    public static final DeferredItem<BlockItem> Endarium_Ore =
            ITEMS.registerSimpleBlockItem("endarium_ore", ModBlocks.Endarium_Ore);

    public static final DeferredItem<BlockItem> Endarium_Raw_Block =
            ITEMS.registerSimpleBlockItem("endarium_raw_block", ModBlocks.Endarium_Raw_Block);

    public static final DeferredItem<Item> Endarium_Raw =
            ITEMS.registerSimpleItem("endarium_raw");

    public static final DeferredItem<Item> Endarium_Ingot =
            ITEMS.registerSimpleItem("endarium_ingot");

    public static final DeferredItem<BlockItem> Endarium_Block =
            ITEMS.registerSimpleBlockItem("endarium_block", ModBlocks.ENDARIUM_BLOCK);


    // Germanium Ore

    public static final DeferredItem<BlockItem> Germanium_of_Block =
            ITEMS.registerSimpleBlockItem("germanium_of_block", ModBlocks.Germanium_of_Block);

    public static final DeferredItem<BlockItem> GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("germanium_ore", ModBlocks.GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> DEEPSLATE_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_germanium_ore", ModBlocks.DEEPSLATE_GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> NETHER_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("nether_germanium_ore", ModBlocks.NETHER_GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> END_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("end_germanium_ore", ModBlocks.END_GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> SMALL_GALAXIS_BUD =
            ITEMS.registerSimpleBlockItem("small_galaxis_bud", ModBlocks.SMALL_GALAXIS_BUD);

    public static final DeferredItem<BlockItem> MEDIUM_GALAXIS_BUD =
            ITEMS.registerSimpleBlockItem("medium_galaxis_bud", ModBlocks.MEDIUM_GALAXIS_BUD);

    public static final DeferredItem<BlockItem> LARGE_GALAXIS_BUD =
            ITEMS.registerSimpleBlockItem("large_galaxis_bud", ModBlocks.LARGE_GALAXIS_BUD);

    public static final DeferredItem<BlockItem> GALAXIS_CLUSTER =
            ITEMS.registerSimpleBlockItem("galaxis_cluster", ModBlocks.GALAXIS_CLUSTER);

    public static final DeferredItem<Item> Galaxis_Quarz =
            ITEMS.registerSimpleItem("galaxis_quarz");

    public static final DeferredItem<BlockItem> BUDDING_GALAXIS =
            ITEMS.registerSimpleBlockItem(
                    "budding_galaxis",
                    ModBlocks.BUDDING_GALAXIS
            );


    public static final DeferredItem<BlockItem> COAL_GENERATOR =
            ITEMS.registerSimpleBlockItem(
                    "coal_generator",
                    ModBlocks.COAL_GENERATOR
            );

    public static final DeferredItem<BlockItem> ARC_FUSION_CHAMBER =
            ITEMS.registerSimpleBlockItem(
                    "arc_fusion_chamber",
                    ModBlocks.ARC_FUSION_CHAMBER
            );

    public static final DeferredItem<BlockItem> ELECTRIC_SMELTER =
            ITEMS.registerSimpleBlockItem(
                    "electric_smelter",
                    ModBlocks.ELECTRIC_SMELTER
            );

    public static final DeferredItem<BlockItem> GERMANIUM_MINER =
            ITEMS.registerSimpleBlockItem(
                    "germanium_miner",
                    ModBlocks.GERMANIUM_MINER
            );



    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
