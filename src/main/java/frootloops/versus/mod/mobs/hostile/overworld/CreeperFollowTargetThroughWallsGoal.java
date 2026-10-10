package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class CreeperFollowTargetThroughWallsGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    public CreeperFollowTargetThroughWallsGoal(Mob mob, Class<T> targetClass, boolean checkVisibility) {
        super(mob, targetClass, checkVisibility);
        this.targetConditions.ignoreLineOfSight();
    }
}