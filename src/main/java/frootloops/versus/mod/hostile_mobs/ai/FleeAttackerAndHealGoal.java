package frootloops.versus.mod.hostile_mobs.ai;
import frootloops.versus.VersusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.PotionItem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FleeAttackerAndHealGoal<T extends LivingEntity> extends Goal {
    protected final PathAwareEntity mob;
    private final double speed;
    @Nullable
    protected Path fleePath;
    protected final EntityNavigation fleeingEntityNavigation;
    @Nullable
    protected LivingEntity targetEntity;

    private int drinkTimeLeft = 24;

    private boolean isDrinkingPotion = false;
    private boolean canDrinkPotion = false;

    public FleeAttackerAndHealGoal(PathAwareEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.fleeingEntityNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }


    @Override
    public boolean canStart() {
        if (mob.hurtTime == 0) return false;
        if (mob.getHealth()/mob.getMaxHealth() > 0.8f) return false;

        targetEntity = (LivingEntity) mob.getRecentDamageSource().getAttacker();
        if (targetEntity == null || !(targetEntity instanceof LivingEntity)) return false;

        Vec3d vec3d = NoPenaltyTargeting.findFrom(mob, 16, 7, targetEntity.getPos());
        if (vec3d == null) return false;
        if (targetEntity.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < targetEntity.squaredDistanceTo(this.mob)) {
            return false;
        }
        fleePath = fleeingEntityNavigation.findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
        return fleePath != null;
    }

    @Override
    public void start() {
        canDrinkPotion = (mob instanceof RaiderEntity || mob.getOffHandStack().getItem() instanceof PotionItem);
        mob.getNavigation().setSpeed(speed);
        fleeingEntityNavigation.startMovingAlong(fleePath, speed);
    }

    @Override
    public boolean shouldContinue() {
        if(fleeingEntityNavigation.isIdle() || targetEntity.squaredDistanceTo(this.mob) > 64.0) {
            if (!isDrinkingPotion && canDrinkPotion) {
                isDrinkingPotion = true;
                drinkTimeLeft = 24;
                if (!mob.isSilent()) mob.getWorld().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_WITCH_DRINK, mob.getSoundCategory(), 1.0f, 1.0f);
            }
            else if (isDrinkingPotion && --this.drinkTimeLeft <= 0) {
                isDrinkingPotion = false;
                canDrinkPotion = false;
                mob.heal(12);
                return false;
            }
        }
        return true;
    }

    @Override
    public void stop() {
        targetEntity = null;
    }
}
