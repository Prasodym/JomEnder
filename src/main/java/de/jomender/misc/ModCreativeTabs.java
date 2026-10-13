package de.jomender.misc;

import de.jomender.Jomender;
import de.jomender.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Jomender.MOD_ID
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JOMENDER_TAB =
            TABS.register("jomender_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.jomender"))
                            .withTabsBefore(CreativeModeTabs.INGREDIENTS)
                            .icon(() -> ModItems.Galaxis_Quarz.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                // Ingots Materials
                                output.accept(ModItems.Germanium.get());
                                output.accept(ModItems.Endarium_Ingot.get());

                                // Clumps Materials
                                output.accept(ModItems.GermaniumClump.get());
                                output.accept(ModItems.Endarium_Raw.get());

                                //Raw Blocks Materials
                                output.accept(ModItems.Germanium_Raw_Block.get());
                                output.accept(ModItems.Endarium_Raw_Block.get());

                                // Blocks Materials
                                output.accept(ModItems.Germanium_of_Block.get());
                                output.accept(ModItems.Endarium_Block.get());
                                output.accept(ModItems.ENDARIUM_UPGRADE_SMITHING_TEMPLATE.get());
                                output.accept(ModItems.ENDARIUM_PICKAXE.get());
                                output.accept(ModItems.ENDARIUM_AXE.get());
                                output.accept(ModItems.ENDARIUM_SHOVEL.get());
                                output.accept(ModItems.ENDARIUM_HOE.get());
                                output.accept(ModItems.ENDARIUM_SHWORD.get());
                                output.accept(ModItems.ENDARIUM_HELMET.get());
                                output.accept(ModItems.ENDARIUM_CHESTPLATE.get());
                                output.accept(ModItems.ENDARIUM_LEGGINGS.get());
                                output.accept(ModItems.ENDARIUM_BOOTS.get());
                                output.accept(ModItems.WEAPON_SCRAP.get());
                                output.accept(ModItems.Endarium_Shword_Shard.get());

                                // Ores Materials
                                output.accept(ModItems.GERMANIUM_ORE.get());
                                output.accept(ModItems.DEEPSLATE_GERMANIUM_ORE.get());
                                output.accept(ModItems.DEEPSLATE_URANIUM_ORE.get());
                                output.accept(ModItems.Raw_Uranium.get());
                                output.accept(ModItems.RAW_URANIUM_BLOCK.get());
                                output.accept(ModItems.Uranium_Dust.get());
                                output.accept(ModItems.Uranium_Ingot.get());
                                output.accept(ModItems.URANIUM_BLOCK.get());
                                output.accept(ModItems.NETHER_GERMANIUM_ORE.get());
                                output.accept(ModItems.END_GERMANIUM_ORE.get());
                                output.accept(ModItems.Endarium_Ore.get());

                                // Misc Materials
                                output.accept(ModItems.BUDDING_GALAXIS.get());
                                output.accept(ModItems.SMALL_GALAXIS_BUD.get());
                                output.accept(ModItems.MEDIUM_GALAXIS_BUD.get());
                                output.accept(ModItems.LARGE_GALAXIS_BUD.get());
                                output.accept(ModItems.GALAXIS_CLUSTER.get());
                                output.accept(ModItems.Galaxis_Quarz.get());
                                output.accept(ModItems.Galaxis_Shard.get());

                                // Upgrades
                                output.accept(ModItems.Speed_upgrade.get());
                                output.accept(ModItems.Efficient_upgrade.get());

                                // Machine Parts
                                output.accept(ModItems.Case.get());
                                output.accept(ModItems.MachineFrame.get());
                                output.accept(ModItems.MachineCore.get());
                                output.accept(ModItems.MachineCircuit.get());
                                output.accept(ModItems.MachineMotor.get());
                                output.accept(ModItems.Magnetic.get());

                                // Machines
                                output.accept(ModItems.COAL_GENERATOR.get());
                                output.accept(ModItems.ELECTRIC_SMELTER.get());
                                output.accept(ModItems.ARC_FUSION_CHAMBER.get());
                                output.accept(ModItems.REACTOR_CONTROLLER.get());
                                output.accept(ModItems.REACTOR_CASING.get());
                                output.accept(ModItems.REACTOR_PORT.get());
                                output.accept(ModItems.REACTOR_GLASS.get());
                                output.accept(ModItems.INFINITE_WATER.get());
                                output.accept(ModItems.NUCLEAR_WASTE.get());
                                output.accept(ModItems.GERMANIUM_MINER.get());
                            })
                            .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
