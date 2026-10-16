package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PathfindToRaidGoal.class)
public abstract class MoveToRaidCenterGoalMixin<T extends Raider> extends Goal {

    @Shadow private final T mob;

    protected MoveToRaidCenterGoalMixin(T actor) {
        this.mob = actor;
    }

    @Override
    public boolean canUse() {
        Raider raider = (Raider)this.mob;
        if(raider.getTarget() == null && !raider.hasControllingPassenger() && raider.hasActiveRaid() && !raider.getCurrentRaid().isOver()){
            if(((ServerLevel)raider.level()).isVillage(raider.blockPosition())) return false;
            return !raider.blockPosition().closerThan(raider.getCurrentRaid().getCenter(), 16.0);
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        Raider raider = (Raider)this.mob;
        if(raider.getTarget() == null && !raider.hasControllingPassenger() && raider.hasActiveRaid() && !raider.getCurrentRaid().isOver()){
            if(((ServerLevel)raider.level()).isVillage(raider.blockPosition())) return false;
            return !raider.blockPosition().closerThan(raider.getCurrentRaid().getCenter(), 16.0);
        }
        return false;
    }
}
