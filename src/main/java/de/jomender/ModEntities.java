package de.jomender;

import de.jomender.entity.GalaxiusEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Jomender.MOD_ID);

    /** Galaxius: Zombie-Magier in Endarium-Rüstung. */
    public static final DeferredHolder<EntityType<?>, EntityType<GalaxiusEntity>> GALAXIUS =
            ENTITIES.registerEntityType("galaxius", GalaxiusEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(GALAXIUS.get(), GalaxiusEntity.createAttributes().build());
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributes);
    }
}
