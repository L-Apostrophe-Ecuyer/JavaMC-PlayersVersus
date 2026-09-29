package frootloops.versus.mixin.items_and_effects.throwing;


import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LargeFireball.class)
public abstract class FireballEntityMixin extends Fireball {
    public FireballEntityMixin(EntityType<? extends Fireball> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        if(this.isInWaterOrRain()) {
            if(this.level() instanceof ServerLevel serverWorld) {
                serverWorld.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 16, 0, 0, 0, 0.3);
            }
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.4F);
            this.discard();
            return;
        }
        super.tick();
    }
}
