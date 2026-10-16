package frootloops.versus.mod.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public class SparksParticle extends SimpleAnimatedParticle {

    protected SparksParticle(ClientLevel world, double x, double y, double z, double vx,  double vy, double vz, SpriteSet spriteProvider) {
        super(world, x, y, z, spriteProvider, 0.165F);
        this.gravity = 0.75F;
        this.xd = vx * 2.0;
        this.zd = vz * 2.0;
        this.yd = Math.abs(vy)/2.0;
        this.quadSize *= 0.75F;
        this.lifetime = 12 + this.random.nextInt(18);
        this.setSpriteFromAge(spriteProvider);
    }

    protected SparksParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider, float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, 0.165F);
        this.gravity = 0.75F;
        this.quadSize *= 0.75F;
        this.lifetime = 12 + this.random.nextInt(18);
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.onGround) {
            if(this.gravity != 0f) {
                this.gravity = 0f;
                this.y += 0.1;
            }
            this.yd = 0f;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, RandomSource random) {
            return new SparksParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
