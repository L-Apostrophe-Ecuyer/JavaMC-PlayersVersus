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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SilverfishEntity.class)
public class SilverfishMixin extends HostileEntity {
    protected SilverfishMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyConstant(method = "createSilverfishAttributes", constant = @Constant(doubleValue = 8.0))
    private static double lessHealth(double hp) {
        return 5.0f;
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
