package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToRaidCenterGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MoveToRaidCenterGoal.class)
public abstract class MoveToRaidCenterGoalMixin<T extends RaiderEntity> extends Goal {

    @Shadow private final T actor;

    protected MoveToRaidCenterGoalMixin(T actor) {
        this.actor = actor;
    }

    @Override
    public boolean canStart() {
        RaiderEntity raider = (RaiderEntity)this.actor;
        if(raider.getTarget() == null && !raider.hasControllingPassenger() && raider.hasActiveRaid() && !raider.getRaid().isFinished()){
            if(((ServerWorld)raider.getWorld()).isNearOccupiedPointOfInterest(raider.getBlockPos())) return false;
            return !raider.getBlockPos().isWithinDistance(raider.getRaid().getCenter(), 16.0);
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        RaiderEntity raider = (RaiderEntity)this.actor;
        if(raider.getTarget() == null && !raider.hasControllingPassenger() && raider.hasActiveRaid() && !raider.getRaid().isFinished()){
            if(((ServerWorld)raider.getWorld()).isNearOccupiedPointOfInterest(raider.getBlockPos())) return false;
            return !raider.getBlockPos().isWithinDistance(raider.getRaid().getCenter(), 16.0);
        }
        return false;
    }
}
