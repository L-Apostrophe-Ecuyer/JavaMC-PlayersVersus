package frootloops.versus.mixin.environment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(NewMinecartBehavior.class)
public abstract class MinecartExperimentalMixin extends MinecartBehavior {


    protected MinecartExperimentalMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Overwrite
    public double getMaxSpeed(ServerLevel world) {
        if(world.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)) {
            double gameruleMaxSpeed = (double)world.getGameRules().getInt(GameRules.RULE_MINECART_MAX_SPEED);
            return (gameruleMaxSpeed == 8.0 ? 64.0 : gameruleMaxSpeed) / (this.minecart.isInWater() ? 40.0 : 20.0);
        }
        return 64.0 / (this.minecart.isInWater() ? 40.0 : 20.0);
    }
}
