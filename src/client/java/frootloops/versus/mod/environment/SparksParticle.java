package frootloops.versus.mod.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class SparksParticle extends AnimatedParticle {

    protected SparksParticle(ClientWorld world, double x, double y, double z, double vx,  double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0.165F);
        this.gravityStrength = 0.75F;
        this.velocityX = vx * 2.0;
        this.velocityZ = vz * 2.0;
        this.velocityY = Math.abs(vy)/2.0;
        this.scale *= 0.75F;
        this.maxAge = 12 + this.random.nextInt(18);
        this.setSpriteForAge(spriteProvider);
    }

    protected SparksParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, 0.165F);
        this.gravityStrength = 0.75F;
        this.scale *= 0.75F;
        this.maxAge = 12 + this.random.nextInt(18);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.onGround) {
            if(this.gravityStrength != 0f) {
                this.gravityStrength = 0f;
                this.y += 0.1;
            }
            this.velocityY = 0f;
        }
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
