package frootloops.versus.mod.hostile_mobs;
;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FleeLastAttackerGoal<T extends LivingEntity> extends Goal {
    protected final PathAwareEntity mob;
    private final double speed;
    @Nullable
    protected Path fleePath;
    protected final EntityNavigation fleeingEntityNavigation;
    @Nullable
    protected LivingEntity targetEntity;

    private int drinkTimeLeft = 24;

    private boolean isDrinkingPotion = false;

    public FleeLastAttackerGoal(PathAwareEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.fleeingEntityNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }


    @Override
    public boolean canStart() {
        if (mob.hurtTime == 0) return false;
        if (mob.getRecentDamageSource().getAttacker() == null) return false;
        if (!(mob.getRecentDamageSource().getAttacker() instanceof LivingEntity)) return false;
        else targetEntity = (LivingEntity) mob.getRecentDamageSource().getAttacker();

        Vec3d vec3d = NoPenaltyTargeting.findFrom(mob, 16, 7, targetEntity.getPos());
        if (vec3d == null) return false;
        if (targetEntity.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < targetEntity.squaredDistanceTo(this.mob)) {
            return false;
        }
        fleePath = fleeingEntityNavigation.findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
        return fleePath != null;
    }

    @Override
    public boolean shouldContinue() {
        if(fleeingEntityNavigation.isIdle()) {
            // Done fleeing, now we can heal with a potion
            if (!isDrinkingPotion) {
                isDrinkingPotion = true;
                drinkTimeLeft = 24;
                mob.equipStack(EquipmentSlot.OFFHAND, mob.getMainHandStack());
                mob.equipStack(EquipmentSlot.MAINHAND, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION));
                if (!mob.isSilent()) {
                    mob.world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_WITCH_DRINK, mob.getSoundCategory(), 1.0f, 1.0f);
                }
            }
            else if (isDrinkingPotion && --this.drinkTimeLeft <= 0) {
                isDrinkingPotion = false;
                mob.equipStack(EquipmentSlot.MAINHAND, mob.getOffHandStack());
                mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                mob.heal(8);
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        fleeingEntityNavigation.startMovingAlong(fleePath, speed);
    }

    @Override
    public void stop() {
        targetEntity = null;
    }

    @Override
    public void tick() {
        mob.getNavigation().setSpeed(speed);
    }
}
