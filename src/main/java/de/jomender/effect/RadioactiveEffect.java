package de.jomender.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Easter Egg: Strahlung durch Uran in der Hand.
 * Macht alle "interval" Ticks 1/2 Herz Schaden. Wie Gift tötet sie nicht - bei 1/2 Herz ist Schluss.
 * Zwei Varianten (siehe ModEffects): Radioactive (Barren) und Highly Radioactive (Block).
 */
public class RadioactiveEffect extends MobEffect {

    private final int interval;

    public RadioactiveEffect(int color, int interval) {
        super(MobEffectCategory.HARMFUL, color);
        this.interval = interval;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            entity.hurtServer(level, entity.damageSources().magic(), 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        int ticks = interval >> amplifier;
        return ticks <= 0 || tickCount % ticks == 0;
    }
}
