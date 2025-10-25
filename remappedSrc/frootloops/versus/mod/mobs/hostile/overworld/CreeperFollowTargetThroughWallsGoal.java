package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;

public class CreeperFollowTargetThroughWallsGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
    public CreeperFollowTargetThroughWallsGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility) {
        super(mob, targetClass, checkVisibility);
        this.targetPredicate.ignoreVisibility();
    }
}