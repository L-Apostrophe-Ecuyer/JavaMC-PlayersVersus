package frootloops.versus.mixin.items_and_effects.equipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FireworkRocketEntity.class)
public abstract class ElytraFireworksSpeedMixin extends Projectile  {

    private static final double INITIAL_SPEED_BOOST = 0.2; // Vanilla is 0.5
    private static final double EXTRA_SPEED_BOOST = 0.1; // Vanilla is 0.5

    public ElytraFireworksSpeedMixin(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow private int lifetime;
    @Shadow @Nullable private LivingEntity attachedToEntity;

    @Override
    public void setOwner(@Nullable Entity entity) {
        super.setOwner(entity);
        if(entity != null && entity instanceof LivingEntity livingEntity && livingEntity.isFallFlying()) {

            // More of a difference between flight 1 and flight 3 (elytra only):
            // Flight 1: Average 16 -> Average 8
            // Flight 2: Average 26 -> Average 21
            // Flight 3: Average 36 -> Average 32
            lifetime = lifetime - 128/(1 + lifetime);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void stopBoostingIfNotHoldingFirework(CallbackInfo info) {
        if(this.attachedToEntity != null && this.attachedToEntity.isFallFlying() && this.level() instanceof ServerLevel serverWorld) {
            if((!this.attachedToEntity.getMainHandItem().is(Items.FIREWORK_ROCKET) && !this.attachedToEntity.getOffhandItem().is(Items.FIREWORK_ROCKET)) || attachedToEntity.horizontalCollision) {
                this.kill(serverWorld);
                info.cancel();
            }
            else if(attachedToEntity.isInWaterOrRain()) this.lifetime--;
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 1))
    private static double lessAccelerationX(double speedAmount) {return 0.02;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 2))
    private static double lessAccelerationY(double speedAmount) {return 0.01;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.1, ordinal = 3))
    private static double lessAccelerationZ(double speedAmount) {return 0.02;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 1))
    private double lessSpeedX(double speedAmount) {return this.tickCount == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 2))
    private double lessSpeedY(double speedAmount) {return this.tickCount == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.5, ordinal = 3))
    private double lessSpeedZ(double speedAmount) {return this.tickCount == 1 ? INITIAL_SPEED_BOOST : EXTRA_SPEED_BOOST;}
}
