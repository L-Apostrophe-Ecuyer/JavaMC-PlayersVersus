package frootloops.versus.mixin.environment;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    public MinecartMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public static boolean areMinecartImprovementsEnabled(World world) {
        return true;// world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        if(cir.getReturnValue() == 8.0) cir.setReturnValue(32.0);
    }

    @ModifyConstant(method = "moveOffRail", constant = @Constant(doubleValue = 0.95))
    private double lessSlowdownWhenInAir(double speedMultiplier) {
        if(this.getVelocity().lengthSquared() < 0.6) return 0.97;
        return 0.96;
    }
}
