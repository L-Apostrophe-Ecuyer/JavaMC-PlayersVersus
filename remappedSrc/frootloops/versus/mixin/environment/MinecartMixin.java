package frootloops.versus.mixin.environment;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    public MinecartMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public static boolean areMinecartImprovementsEnabled(World world) {
        return world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        if(cir.getReturnValue() == 8.0) cir.setReturnValue(32.0);
    }
}
