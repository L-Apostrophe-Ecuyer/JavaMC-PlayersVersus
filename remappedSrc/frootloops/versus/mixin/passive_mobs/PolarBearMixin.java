package frootloops.versus.mixin.passive_mobs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PolarBearEntity.class)
public abstract class PolarBearMixin extends PassiveEntity {

    protected PolarBearMixin(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "createPolarBearAttributes", at = @At("HEAD"), cancellable = true)
    private static void createPolarBearAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
                MobEntity.createMobAttributes()
                        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36f)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
                        .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0));
    }
}
