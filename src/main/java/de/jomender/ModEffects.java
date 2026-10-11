package de.jomender;

import de.jomender.effect.RadioactiveEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Jomender.MOD_ID);

    /** Uran-Barren in der Hand. Icon: textures/mob_effect/radioactive.png (grün) */
    public static final DeferredHolder<MobEffect, RadioactiveEffect> RADIOACTIVE =
            MOB_EFFECTS.register("radioactive", () -> new RadioactiveEffect(0x6EDC28, 40));

    /** Uran-Block in der Hand. Icon: textures/mob_effect/highly_radioactive.png (gelb) */
    public static final DeferredHolder<MobEffect, RadioactiveEffect> HIGHLY_RADIOACTIVE =
            MOB_EFFECTS.register("highly_radioactive", () -> new RadioactiveEffect(0xF0C81E, 20));

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
