package frootloops.versus.mixin.items_and_effects.throwing;


import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireballEntity.class)
public abstract class FireballEntityMixin extends AbstractFireballEntity {
    public FireballEntityMixin(EntityType<? extends AbstractFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        if(this.isTouchingWaterOrRain()) {
            if(this.getEntityWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 16, 0, 0, 0, 0.3);
            }
            this.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.4F);
            this.discard();
            return;
        }
        super.tick();
    }
}
