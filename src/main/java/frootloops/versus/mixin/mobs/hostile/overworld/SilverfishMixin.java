package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SilverfishEntity.class)
public class SilverfishMixin extends HostileEntity {
    protected SilverfishMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean hasAttacked = super.tryAttack(world, target);
        if (hasAttacked && target instanceof LivingEntity livingEntity && this.getRandom().nextBoolean()) {
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.INFESTED, 240), this);
        }
        return hasAttacked;
    }
}
