package de.jomender.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class EndariumLightningParticle extends SingleQuadParticle {

    private EndariumLightningParticle(ClientLevel level, double x, double y, double z,
                                     SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, sprites.get(0, 1));
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = 3 + random.nextInt(3); // 0,15 bis 0,25 Sekunden
        this.quadSize = 0.25f + random.nextFloat() * 0.17f;
        this.roll = random.nextFloat() * (float) (Math.PI * 2.0);
        this.oRoll = this.roll;
        if (random.nextBoolean()) {
            this.setColor(0.55f, 0.94f, 1.0f); // cyan
        } else {
            this.setColor(0.84f, 0.56f, 1.0f); // violett
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0; // Blitz bleibt auch im Dunkeln sichtbar
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setAlpha(Math.max(0.0f, 1.0f - this.age / (float) this.lifetime));
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            return new EndariumLightningParticle(level, x, y, z, sprites, random);
        }
    }
}
