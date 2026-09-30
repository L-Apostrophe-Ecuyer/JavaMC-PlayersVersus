package frootloops.versus.mod.mobs.hostile.nether;

import frootloops.versus.VersusMod;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;

public class WildfireShootFireBallsGoal extends Goal {

    private final static int MAX_FIREBALLS = 3;
    private final WildfireEntity wildfireEntity;
    private int fireballsFired;
    private int ticksGroundPound;
    private int ticksRingOfFire;
    private int fireballCooldown;
    private int targetNotVisibleTicks;

    public WildfireShootFireBallsGoal(WildfireEntity wildfireEntity) {
        this.wildfireEntity = wildfireEntity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity livingEntity = this.wildfireEntity.getTarget();
        return livingEntity != null && livingEntity.isAlive() && this.wildfireEntity.canAttack(livingEntity);
    }

    @Override
    public void start() {
        fireballsFired = 0;
        ticksRingOfFire = 0;
        ticksGroundPound = 0;
    }

    @Override
    public void stop() {
        this.targetNotVisibleTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.fireballCooldown--;
        if(fireballCooldown == 40) {
            this.wildfireEntity.setFireActive(true);
        }

        LivingEntity livingEntity = this.wildfireEntity.getTarget();
        if (livingEntity != null) {
            boolean isPlayerVisible = this.wildfireEntity.getSensing().hasLineOfSight(livingEntity);
            if (isPlayerVisible) this.targetNotVisibleTicks = 0;
            else this.targetNotVisibleTicks++;

            double squaredDistanceTo = this.wildfireEntity.distanceToSqr(livingEntity);
            if(!wildfireEntity.isOnFire()) {
                if(fireballCooldown <= 0) wildfireEntity.setFireActive(true);
                else {
                    if(!wildfireEntity.getMoveControl().hasWanted()) {
                        if(squaredDistanceTo > 25.0 && squaredDistanceTo < 512.0) this.wildfireEntity.getMoveControl().setWantedPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 1.0);
                        else this.wildfireEntity.push(0.0, wildfireEntity.getDeltaMovement().y > 0.15 ? 0.0 : 0.05, 0.0);
                    }
                    return;
                }
            }
            else if(!wildfireEntity.getMoveControl().hasWanted() && squaredDistanceTo < 64.0) {
                this.wildfireEntity.getMoveControl().setWantedPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 1.0);
            }

            if (squaredDistanceTo < this.getFollowRange() * this.getFollowRange() && isPlayerVisible) {
                if (this.fireballCooldown <= 0) {
                    double dx = livingEntity.getX() - this.wildfireEntity.getX();
                    double dy = livingEntity.getY(0.5) - this.wildfireEntity.getY(0.5);
                    double dz = livingEntity.getZ() - this.wildfireEntity.getZ();

                    // If in the middle of shooting fireballs, keep at it:
                    if(fireballsFired > 0) this.shootBigFireballAtPlayer(squaredDistanceTo, dx, dy, dz);

                    // If in the middle of a ground pound:
                    else if(ticksGroundPound > 0) this.doGroundPound();

                    // If in the middle of a ring of fire attack:
                    else if(ticksRingOfFire > 0) this.shootSmallFireballsInSpray(dy);

                    // Else, start a new attack:
                    else {
                        // Decide randomly which attack to select:
                        int weightBigFireball = squaredDistanceTo > 512.0 ? 6 : squaredDistanceTo > 16.0 ? 2 : 0;
                        int weightSmallFireballs = squaredDistanceTo < 25.0 ? 1 : squaredDistanceTo < 384.0 && dy * dy < 4.0 ? 5 : 0;
                        int weightGroundPound = (squaredDistanceTo < 25.0 || (dy < 0 && dx * dx + dz * dz < 16.0))  ? 8 : 0;
                        int random = wildfireEntity.getRandom().nextInt(weightGroundPound + weightSmallFireballs + weightBigFireball);
                        if (random <= weightBigFireball) {
                            this.shootBigFireballAtPlayer(squaredDistanceTo, dx, dy, dz);
                        }
                        else if (random - weightBigFireball <= weightSmallFireballs) {
                            this.shootSmallFireballsInSpray(dy);
                        }
                        else {
                            this.doGroundPound();
                        }
                    }
                }
                this.wildfireEntity.getLookControl().setLookAt(livingEntity, 10.0F, 10.0F);

            } else if (this.targetNotVisibleTicks < 5) {
                this.wildfireEntity.getMoveControl().setWantedPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 1.0);
            }

            super.tick();
        }
    }

    private double getFollowRange() {
        return this.wildfireEntity.getAttributeValue(Attributes.FOLLOW_RANGE);
    }


    /**
     * Attack numero uno: big fireballs
     */
    private void shootBigFireballAtPlayer(double squaredDistanceTo, double dx, double dy, double dz) {
        // Play sound:
        if (!this.wildfireEntity.isSilent()) this.wildfireEntity.level().levelEvent(null, LevelEvent.SOUND_BLAZE_FIREBALL, this.wildfireEntity.blockPosition(), 0);

        // Shoot fireball:
        double deviation = 0.5 + squaredDistanceTo/128 - 1/Math.max(2, 256 - squaredDistanceTo);
        Vec3 velocity = new Vec3(this.wildfireEntity.getRandom().triangle(dx, deviation), dy, this.wildfireEntity.getRandom().triangle(dz, deviation));
        LargeFireball fireball = new LargeFireball(this.wildfireEntity.level(), this.wildfireEntity, velocity.normalize(), 1);
        fireball.setPos(fireball.getX(), this.wildfireEntity.getY(0.5) + 0.5, fireball.getZ());
        this.wildfireEntity.level().addFreshEntity(fireball);

        // Cooldown:
        this.fireballsFired++;
        this.fireballCooldown = 6;
        if(this.fireballsFired > MAX_FIREBALLS) {
            wildfireEntity.setFireActive(false);
            this.fireballCooldown = 120;
            this.fireballsFired = 0;
        }
    }

    /**
     * Attack numero dos: a ton of small fireballs
     */
    private void shootSmallFireballsInSpray(double dy) {
        // If out of ticks, cancel attack:
        if(ticksRingOfFire == 0) ticksRingOfFire = 20;
        ticksRingOfFire--;
        if(ticksRingOfFire == 0) return;

        // If not on same y level as player, try to adjust height:
        if(dy * dy > 3.0) {
            wildfireEntity.push(0.0, (dy * 3)/(8 + dy * dy), 0.0);
            return;
        }

        // Play sound:
        if (!this.wildfireEntity.isSilent()) this.wildfireEntity.level().levelEvent(null, LevelEvent.SOUND_BLAZE_FIREBALL, this.wildfireEntity.blockPosition(), 0);

        // Shoot fireballs:
        for(int i = 0; i < 4; i++) {
            for (double vxRatio = 0.0; vxRatio <= 1.0; vxRatio += 0.1) {
                double vx = vxRatio;
                if(i % 2 == 0) vx = -vx;

                double vz = 1.0 - vxRatio;
                if(i >= 2) vz = -vz;

                double vy = 0.0 - 0.2 * (wildfireEntity.getRandom().nextDouble() - wildfireEntity.getRandom().nextDouble());
                SmallFireball fireball = new SmallFireball(this.wildfireEntity.level(), this.wildfireEntity, new Vec3(vx/2.0, vy, vz/2.0));
                fireball.setPos(fireball.getX(), this.wildfireEntity.getY(0.5) + 0.5, fireball.getZ());
                this.wildfireEntity.level().addFreshEntity(fireball);
            }
        }

        // Cooldown:
        this.fireballCooldown = 80;
        wildfireEntity.setFireActive(false);
    }

    /**
     * Attack numero tres: big ground pound
     */
    private void doGroundPound() {
        if(this.ticksGroundPound == 0) {
            ticksGroundPound = 20;
            this.wildfireEntity.addSoulFlameParticles(25 - ticksGroundPound);
        }
        else if(this.ticksGroundPound > 1) {
            this.wildfireEntity.push(0.0, 0.1, 0.0);
            if(ticksGroundPound % 5 == 0) this.wildfireEntity.addSoulFlameParticles(25 - ticksGroundPound);
            ticksGroundPound--;
        }
        else if(!wildfireEntity.onGround()) {
            this.wildfireEntity.push(0.0, -1.0, 0.0);
        }
        else {
            this.wildfireEntity.setPermanentlyInvulnerable(true);
            this.wildfireEntity.level().explode(wildfireEntity, wildfireEntity.getX(), wildfireEntity.getY(), wildfireEntity.getZ(), 2, true, Level.ExplosionInteraction.MOB);
            this.wildfireEntity.setPermanentlyInvulnerable(false);

            // Long Cooldown (5-8s)
            this.ticksGroundPound = 0;
            this.fireballCooldown = 100 + wildfireEntity.getRandom().nextInt(60);
            wildfireEntity.setFireActive(false);
        }
    }
}