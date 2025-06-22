package frootloops.versus.mixin.environment;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.vehicle.MinecartController;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(ExperimentalMinecartController.class)
public abstract class MinecartExperimentalMixin extends MinecartController {


    protected MinecartExperimentalMixin(AbstractMinecartEntity minecart) {
        super(minecart);
    }

    @Overwrite
    public double getMaxSpeed(ServerWorld world) {
        if(world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)) {
            double gameruleMaxSpeed = (double)world.getGameRules().getInt(GameRules.MINECART_MAX_SPEED);
            return (gameruleMaxSpeed == 8.0 ? 64.0 : gameruleMaxSpeed) / (this.minecart.isTouchingWater() ? 40.0 : 20.0);
        }
        return 64.0 / (this.minecart.isTouchingWater() ? 40.0 : 20.0);
    }
}
