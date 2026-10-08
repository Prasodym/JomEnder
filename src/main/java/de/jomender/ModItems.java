package de.jomender;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Jomender.MOD_ID);

    public static final DeferredItem<Item> Germanium =
            ITEMS.registerSimpleItem("germanium_ingot");

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

    public static final DeferredItem<Item> GermaniumClumb =
            ITEMS.registerSimpleItem("germanium_clumb");

    public static final DeferredItem<BlockItem> GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("germanium_ore", ModBlocks.GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> DEEPSLATE_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_germanium_ore", ModBlocks.DEEPSLATE_GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> NETHER_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("nether_germanium_ore", ModBlocks.NETHER_GERMANIUM_ORE);

    public static final DeferredItem<BlockItem> END_GERMANIUM_ORE =
            ITEMS.registerSimpleBlockItem("end_germanium_ore", ModBlocks.END_GERMANIUM_ORE);


    public static final DeferredItem<BlockItem> COAL_GENERATOR =
            ITEMS.registerSimpleBlockItem(
                    "coal_generator",
                    ModBlocks.COAL_GENERATOR
            );

    public static final DeferredItem<BlockItem> ELECTRIC_SMELTER =
            ITEMS.registerSimpleBlockItem(
                    "electric_smelter",
                    ModBlocks.ELECTRIC_SMELTER
            );


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
