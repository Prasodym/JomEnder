package de.jomender;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Jomender.MOD_ID);

    public static final Supplier<SimpleParticleType> ENDARIUM_LIGHTNING =
            PARTICLE_TYPES.register("endarium_lightning", () -> new SimpleParticleType(false));
}
