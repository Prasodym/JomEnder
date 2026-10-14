package de.jomender;

import de.jomender.armor.EndariumArmor;
import de.jomender.armor.EndariumArmorItem;
import de.jomender.item.EndariumTools;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.ArmorType;
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
    public static final DeferredItem<Item> Endarium_Shword_Shard =
            ITEMS.registerSimpleItem("endarium_shword_shard");

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

    // Angriffswerte wie bei Netherite - das Endarium-Material legt selbst noch +1 Schaden drauf.
    public static final DeferredItem<AxeItem> ENDARIUM_AXE =
            ITEMS.registerItem("endarium_axe",
                    props -> new AxeItem(EndariumTools.ENDARIUM, 5.0F, -3.0F, props),
                    props -> props
                            .fireResistant()
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                            .rarity(Rarity.EPIC));

    public static final DeferredItem<ShovelItem> ENDARIUM_SHOVEL =
            ITEMS.registerItem("endarium_shovel",
                    props -> new ShovelItem(EndariumTools.ENDARIUM, 1.5F, -3.0F, props),
                    props -> props
                            .fireResistant()
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                            .rarity(Rarity.EPIC));

    /**
     * Endarium-Schwert: unzerstörbar, 50 Angriffsschaden.
     * Rechnung: 1 (Spieler) + 44 (hier) + 5 (Endarium-Material) = 50. Tempo wie Netherite-Schwert.
     */
    public static final DeferredItem<Item> ENDARIUM_SHWORD =
            ITEMS.registerItem("endarium_shword", Item::new, props -> props
                    .sword(EndariumTools.ENDARIUM, 44.0F, -2.4F)
                    .fireResistant()
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                    .rarity(Rarity.EPIC));

    // =========================
    // ENDARIUM-RÜSTUNG (upgradebar am Schmiedetisch, siehe armor/ArmorUpgrade)
    // =========================
    public static final DeferredItem<EndariumArmorItem> ENDARIUM_HELMET = endariumArmor("endarium_helmet", ArmorType.HELMET);
    public static final DeferredItem<EndariumArmorItem> ENDARIUM_CHESTPLATE = endariumArmor("endarium_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<EndariumArmorItem> ENDARIUM_LEGGINGS = endariumArmor("endarium_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<EndariumArmorItem> ENDARIUM_BOOTS = endariumArmor("endarium_boots", ArmorType.BOOTS);

    private static DeferredItem<EndariumArmorItem> endariumArmor(String name, ArmorType type) {
        return ITEMS.registerItem(name, EndariumArmorItem::new, props -> props
                .humanoidArmor(EndariumArmor.MATERIAL, type)
                .fireResistant()
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .rarity(Rarity.EPIC));
    }

    public static final DeferredItem<HoeItem> ENDARIUM_HOE =
            ITEMS.registerItem("endarium_hoe",
                    props -> new HoeItem(EndariumTools.ENDARIUM, -4.0F, 0.0F, props),
                    props -> props
                            .fireResistant()
                            .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                            .rarity(Rarity.EPIC));

    /** Drop vom Wither (50 %), schimmert wie verzaubert. Textur: textures/item/wapon_scrap.png */
    public static final DeferredItem<Item> WEAPON_SCRAP =
            ITEMS.registerItem("weapon_scrap", Item::new, props -> props
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .fireResistant()
                    .rarity(Rarity.RARE));

    public static final DeferredItem<SmithingTemplateItem> ENDARIUM_UPGRADE_SMITHING_TEMPLATE =
            ITEMS.registerItem("endarium_upgrade_smithing_template",
                    EndariumTools::createUpgradeTemplate,
                    props -> props.rarity(Rarity.EPIC));

    /** Atommüll aus dem Reaktor - radioaktiv in der Hand. */
    public static final DeferredItem<Item> NUCLEAR_WASTE =
            ITEMS.registerSimpleItem("nuclear_waste");

    public static final DeferredItem<BlockItem> REACTOR_CASING =
            ITEMS.registerSimpleBlockItem("reactor_casing", ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_CONTROLLER =
            ITEMS.registerSimpleBlockItem("reactor_controller", ModBlocks.REACTOR_CONTROLLER);
    public static final DeferredItem<BlockItem> REACTOR_PORT =
            ITEMS.registerSimpleBlockItem("reactor_port", ModBlocks.REACTOR_PORT);
    public static final DeferredItem<BlockItem> REACTOR_GLASS =
            ITEMS.registerSimpleBlockItem("reactor_glass", ModBlocks.REACTOR_GLASS);
    public static final DeferredItem<BlockItem> INFINITE_WATER =
            ITEMS.registerSimpleBlockItem("infinite_water", ModBlocks.INFINITE_WATER);

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
    public static final DeferredItem<Item> Galaxis_Shard =
            ITEMS.registerSimpleItem("galaxis_shard");

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

    public static final DeferredItem<BlockItem> CRUSHER =
            ITEMS.registerSimpleBlockItem(
                    "crusher",
                    ModBlocks.CRUSHER
            );

    // =========================
    // STAUB (aus dem Crusher)
    // =========================

    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> GERMANIUM_DUST = ITEMS.registerSimpleItem("germanium_dust");
    public static final DeferredItem<Item> ENDARIUM_DUST = ITEMS.registerSimpleItem("endarium_dust");

    /** Entwickler-Werkzeug: Spawner auf Galaxius umstellen / Galaxius beschwören. */
    public static final DeferredItem<de.jomender.item.DevToolItem> DEV_TOOL =
            ITEMS.registerItem("dev_tool", de.jomender.item.DevToolItem::new, props -> props
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<BlockItem> GERMANIUM_MINER =
            ITEMS.registerSimpleBlockItem(
                    "germanium_miner",
                    ModBlocks.GERMANIUM_MINER
            );



    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
