package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Silverfish.class)
public class SilverfishMixin extends Monster {
    protected SilverfishMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyConstant(method = "createAttributes", constant = @Constant(doubleValue = 8.0))
    private static double lessHealth(double hp) {
        return 3.0f;
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean hasAttacked = super.doHurtTarget(world, target);
        if (hasAttacked && target instanceof LivingEntity livingEntity && this.getRandom().nextInt(4) == 1) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.INFESTED, 80), this);
        }
        return hasAttacked;
    }
}
