package frootloops.versus.mixin.mobs.passive;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PolarBear.class)
public abstract class PolarBearMixin extends AgeableMob {

    protected PolarBearMixin(EntityType<? extends AgeableMob> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void createPolarBearAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(
                Mob.createMobAttributes()
                        .add(Attributes.FOLLOW_RANGE, 30.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.36f)
                        .add(Attributes.ATTACK_DAMAGE, 12.0)
                        .add(Attributes.MAX_HEALTH, 60.0));
    }
}
