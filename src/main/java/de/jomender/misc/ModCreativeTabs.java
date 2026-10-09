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
                                output.accept(ModItems.Germanium.get());
                                output.accept(ModItems.GermaniumClumb.get());
                                output.accept(ModItems.Germanium_Raw_Block.get());
                                output.accept(ModItems.Germanium_of_Block.get());
                                output.accept(ModItems.GERMANIUM_ORE.get());
                                output.accept(ModItems.DEEPSLATE_GERMANIUM_ORE.get());
                                output.accept(ModItems.NETHER_GERMANIUM_ORE.get());
                                output.accept(ModItems.END_GERMANIUM_ORE.get());
                                output.accept(ModItems.Endarium_Ore.get());
                                output.accept(ModItems.BUDDING_GALAXIS.get());
                                output.accept(ModItems.SMALL_GALAXIS_BUD.get());
                                output.accept(ModItems.MEDIUM_GALAXIS_BUD.get());
                                output.accept(ModItems.LARGE_GALAXIS_BUD.get());
                                output.accept(ModItems.GALAXIS_CLUSTER.get());
                                output.accept(ModItems.Galaxis_Quarz.get());
                                output.accept(ModItems.Case.get());
                                output.accept(ModItems.MachineFrame.get());
                                output.accept(ModItems.MachineCore.get());
                                output.accept(ModItems.MachineCircuit.get());
                                output.accept(ModItems.MachineMotor.get());
                                output.accept(ModItems.Magnetic.get());
                                output.accept(ModItems.COAL_GENERATOR.get());
                                output.accept(ModItems.ELECTRIC_SMELTER.get());
                                output.accept(ModItems.GERMANIUM_MINER.get());
                            })
                            .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
