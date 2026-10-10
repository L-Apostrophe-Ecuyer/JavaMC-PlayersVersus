package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.Combat;
import java.util.EnumSet;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Holds a mob still while a player has it in view, as the pale_creeper's creeping does ({@link CreepingAndExplodingGoal}):
 * it only creeps up while unseen. It stops pretending within {@link #POUNCE_DISTANCE} of its target, and for a while
 * after it's hurt. Takes over moving and looking, so put it ahead of the mob's attack and wandering goals.
 */
public class FreezeWhenWatchedGoal extends Goal {
    /** Within this of its target, it attacks whether watched or not. */
    static final double POUNCE_DISTANCE = 4.0;
    /** Players farther than this don't hold it still. */
    static final double WATCHED_FROM = 48.0;

    private final Mob mob;

    public FreezeWhenWatchedGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.watched();
    }

    @Override
    public boolean canContinueToUse() {
        return this.watched();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.mob.getNavigation().stop();
    }

    /** A player sees it (looking its way, nothing in between), and it neither was just hurt nor is close enough to pounce. */
    private boolean watched() {
        if (this.mob.getLastHurtByMob() != null || this.mob.isInLiquid()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target != null && this.mob.distanceToSqr(target) <= POUNCE_DISTANCE * POUNCE_DISTANCE) return false;
        for (Player player : this.mob.level().players()) {
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) || !player.isAlive() || !player.closerThan(this.mob, WATCHED_FROM)) continue;
            if (Combat.isLookingTowards(player, this.mob.position()) && player.hasLineOfSight(this.mob)) return true;
        }
        return false;
    }
}
