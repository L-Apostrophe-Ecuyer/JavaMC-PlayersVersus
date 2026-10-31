package frootloops.versus.mod.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Buoyancy's effect particle: a small bubble that drifts up from the entity and pops (its sprites, bubble then pop, in
 * {@code particles/buyancy_effect.json}). Effect particles come with a speed of 1 each way, which it ignores, as
 * vanilla's own effect particle does.
 */
@Environment(EnvType.CLIENT)
public class BuoyancyParticle extends SimpleAnimatedParticle {

    protected BuoyancyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, -0.02F);
        this.xd = (this.random.nextDouble() - 0.5) * 0.02;
        this.yd = 0.02 + this.random.nextDouble() * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.02;
        this.quadSize *= 0.6F;
        this.lifetime = 16 + this.random.nextInt(8);
        this.setSpriteFromAge(sprites);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, RandomSource random) {
            return new BuoyancyParticle(level, x, y, z, this.sprites);
        }
    }
}
