package frootloops.versus.mixin.items_and_effects.equipment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FireworkRocketEntity.class)
public abstract class ElytraFireworksSpeedMixin extends ProjectileEntity  {

    private static final double INITIAL_SPEED_BOOST = 0.2; // Vanilla is 0.5
    private static final double EXTRA_SPEED_BOOST = 0.1; // Vanilla is 0.5

    public ElytraFireworksSpeedMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow private int lifeTime;
    @Shadow @Nullable private LivingEntity shooter;

    @Override
    public void setOwner(@Nullable Entity entity) {
        super.setOwner(entity);
        if(entity != null && entity instanceof LivingEntity livingEntity && livingEntity.isGliding()) {

            // More of a difference between flight 1 and flight 3 (elytra only):
            // Flight 1: Average 16 -> Average 8
            // Flight 2: Average 26 -> Average 21
            // Flight 3: Average 36 -> Average 32
            lifeTime = lifeTime - 128/(1 + lifeTime);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void stopBoostingIfNotHoldingFirework(CallbackInfo info) {
        if(this.shooter != null && this.shooter.isGliding() && this.getEntityWorld() instanceof ServerWorld serverWorld) {
            if((!this.shooter.getMainHandStack().isOf(Items.FIREWORK_ROCKET) && !this.shooter.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) || shooter.horizontalCollision) {
                this.kill(serverWorld);
                info.cancel();
            }
            else if(shooter.isTouchingWaterOrRain()) this.lifeTime--;
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 1))
    private static double lessAccelerationX(double speedAmount) {return 0.02;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 2))
    private static double lessAccelerationY(double speedAmount) {return 0.01;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 3))
    private static double lessAccelerationZ(double speedAmount) {return 0.02;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 1))
    private double lessSpeedX(double speedAmount) {return this.age == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 2))
    private double lessSpeedY(double speedAmount) {return this.age == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 3))
    private double lessSpeedZ(double speedAmount) {return this.age == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
}
