package frootloops.versus.mod.mobs.hostile.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;

import java.util.function.Predicate;

public class FollowTargetThroughWallsGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
    public FollowTargetThroughWallsGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility) {
        super(mob, targetClass, checkVisibility);
        this.targetPredicate.ignoreVisibility();
    }
}