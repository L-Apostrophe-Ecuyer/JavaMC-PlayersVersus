package frootloops.versus.mod.mobs.hostile.overworld;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class FleeAttackerAndHealGoal<T extends LivingEntity> extends Goal {
    protected final PathfinderMob mob;
    private final double speed;
    @Nullable
    protected Path fleePath;
    protected final PathNavigation fleeingEntityNavigation;
    @Nullable
    protected LivingEntity targetEntity;

    private int drinkTimeLeft = 24;

    private boolean isDrinkingPotion = false;
    private boolean canDrinkPotion = false;

    public FleeAttackerAndHealGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.fleeingEntityNavigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }


    @Override
    public boolean canUse() {
        if (mob.hurtTime == 0) return false;
        if (mob.getHealth()/mob.getMaxHealth() > 0.5f) return false;
        if (mob.getLastDamageSource() == null) return false;

        targetEntity = (LivingEntity) mob.getLastDamageSource().getEntity();
        if (targetEntity == null || !(targetEntity instanceof LivingEntity)) return false;

        Vec3 vec3d = DefaultRandomPos.getPosAway(mob, 16, 7, targetEntity.position());
        if (vec3d == null) return false;
        if (targetEntity.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < targetEntity.distanceToSqr(this.mob)) {
            return false;
        }
        fleePath = fleeingEntityNavigation.createPath(vec3d.x, vec3d.y, vec3d.z, 0);
        return fleePath != null;
    }

    @Override
    public void start() {
        canDrinkPotion = (mob instanceof Witch || mob.getOffhandItem().getItem() instanceof PotionItem);
        mob.getNavigation().setSpeedModifier(speed);
        fleeingEntityNavigation.moveTo(fleePath, speed);
    }

    @Override
    public boolean canContinueToUse() {
        if(fleeingEntityNavigation.isDone() || targetEntity.distanceToSqr(this.mob) > 64.0) {
            if (!isDrinkingPotion && canDrinkPotion) {
                isDrinkingPotion = true;
                drinkTimeLeft = 32;
                if (!mob.isSilent()) mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WITCH_DRINK, mob.getSoundSource(), 1.0f, 1.0f);
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
        mob.setTarget(targetEntity);
    }
}
