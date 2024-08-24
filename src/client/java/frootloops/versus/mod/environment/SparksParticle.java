package frootloops.versus.mod.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class SparksParticle extends AnimatedParticle {

    protected SparksParticle(ClientWorld world, double x, double y, double z, double vx,  double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0.125F);
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.scale *= 0.75F;
        this.maxAge = 8 + this.random.nextInt(8);
        this.setSpriteForAge(spriteProvider);
    }

    protected SparksParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, 0.125F);
        this.scale *= 0.75F;
        this.maxAge = 8 + this.random.nextInt(8);
        this.setSpriteForAge(spriteProvider);
    }


    @Override
    public void move(double dx, double dy, double dz) {
        this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
        this.repositionFromBoundingBox();
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new SparksParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
