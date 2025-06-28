package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.WardenAngerManager;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angriness;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenBrain;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(WardenEntity.class)
public class WardenMixin extends HostileEntity {

    private static final int BOOM_COOLDOWN_AFTER_ATTACK = 240;
    private static final double SNIFF_RANGE_SQUARED = 36.0d, SNIFF_RANGE_VERTICAL = 8.0d;
    private static final double BOOM_RANGE_HORIZONTAL = 8.0d, BOOM_RANGE_VERTICAL = 10.0d,
            BOOM_RANGE_HORIZONTAL_SQUARED = BOOM_RANGE_HORIZONTAL * BOOM_RANGE_HORIZONTAL;

    protected WardenMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }


    @Shadow
    @Contract("null->false")
    public boolean isValidTarget(@Nullable Entity entity) {
        if (entity instanceof LivingEntity livingEntity
                && this.getWorld() == entity.getWorld()
                && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity)
                && !this.isTeammate(entity)
                && livingEntity.getType() != EntityType.ARMOR_STAND
                && livingEntity.getType() != EntityType.WARDEN
                && !livingEntity.isInvulnerable()
                && !livingEntity.isDead()) {
            return true;
        }
        return false;
    }

    @Shadow
    private boolean isDiggingOrEmerging() {return false;}

    @Shadow
    public void updateAttackTarget(LivingEntity target) {}


    /**
     *  - ANGER TRACKING TWEAKED (NERFED)
     * Wardens will quickly lose sight (haha) of their targets, meaning if you run away
     * and crouch or hide again, the Warden won't be able to pinpoint where you are anymore.
     * This is to incentive other play styles, like Tom & Jerry chases, rather than just
     * running away and waiting.
     *
     * However! On spawning, they'll be immediately suspicious of the nearest player, and
     * will start walking in their direction.
     */
    @Shadow
    private WardenAngerManager angerManager;

    @Shadow
    public Angriness getAngriness() {
        return Angriness.getForAnger(this.getAngerAtTarget());
    }

    @Shadow
    private int getAngerAtTarget() {
        return this.angerManager.getAngerFor(this.getTarget());
    }


    /**
    This allows players to sneak away from Wardens, and Wardens to lose their scent, making for tense encounters.
     */
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void reduceAngerTowardsSneakyPlayers(CallbackInfo ci){
        if(this.getAngriness() == Angriness.ANGRY && this.age > 200 && this.age % 3 == 0) {
            Entity target = this.getTarget();
            if(target != null && (target.isSneaky() || target.squaredDistanceTo(this.getPos()) > 400)) {
                this.angerManager.increaseAngerAt(target, -1);
            }
        }
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void dontDespawnWhenSummonedByCheats(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable cir){
        if (spawnReason != SpawnReason.TRIGGERED && spawnReason != SpawnReason.SPAWNER) {
            this.setHealth(300.0f); // Bit easier to kill compared to regular shrieker Wardens
            this.setPersistent();
        }
        else if(spawnReason == SpawnReason.TRIGGERED) {
            PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, 64.0d);
            if(closestPlayer != null && !this.isInRange(closestPlayer, 16.0)) {
                WardenBrain.lookAtDisturbance((WardenEntity) ((Object)this), closestPlayer.getBlockPos());
                this.angerManager.increaseAngerAt(closestPlayer, closestPlayer.isSprinting() ? 70 : 50);
            }
        }
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.TRIGGERED && this.getBlockPos().getY() > -32) return false;
        else return super.canSpawn(world, spawnReason);
    }

    /**
     *  - ANGER TOWARDS PLAYERS REDUCED
     * The anger amount from hearing or sniffing players is now mostly reduced, and *depends on distance*.
     * This is to rebalance the fact that Wardens have a much wider range for hearing vibrations.
     */
    @Overwrite
    public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {
        if (!this.isAiDisabled() && this.isValidTarget(entity)) {
            boolean isPlayer = entity instanceof PlayerEntity;

            // Reset dig cooldown:
            if (this.getBrain().hasMemoryModule(MemoryModuleType.DIG_COOLDOWN)) {
                this.getBrain().remember(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, isPlayer ? 1200L : 600L);
            }

            // If Player:
            if(isPlayer && amount > 0) {

                // If current target isnt a player, but new one is, forget them and prioritize player:
                if( this.getTarget() != null && !(this.getTarget() instanceof PlayerEntity)) {
                    if(this.getAngriness().isAngry()) this.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
                    this.angerManager.increaseAngerAt(this.getTarget(), -4 * amount);
                }

                // Get rebalanced anger amount, for regular sniffing or hearing:
                else if(amount == 10 || amount == 35) {
                    if(entity.isSprinting()) amount = 40;
                    else {
                        double squaredDistance = this.squaredDistanceTo(entity);
                        if(squaredDistance > 24.0) amount /= 5;
                        else if (squaredDistance > 16.0) amount /= 2;
                        else if (squaredDistance < 4.0) amount = 3 + amount/2;
                    }
                }
            }

            // Increase anger:
            this.angerManager.increaseAngerAt(entity, amount);

            // Play sound:
            if (listening && !this.isInPose(EntityPose.ROARING)) {
                this.playSound(this.getAngriness().getListeningSound(), 10.0F, this.getSoundPitch());
            }
        }
    }



    /***
     *  - SONIC BOOM NERFED & SNIFFING NERFED
     * Ranged sonic boom attacks have a shorter range. This allows for closer encounters,
     * fewer frustrating deaths, and things like arrow invulnerability.You want to know
     * why you're being attacked, and have a way to avoiding it. In vanilla, these sonic
     * booms are simply too punishing.
     */
    @Override
    public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
        double deltaX = entity.getX() - this.getX();
        double deltaY = entity.getY() - this.getY();
        double deltaZ = entity.getZ() - this.getZ();

        // Sniffing:
        if(horizontalRadius == 6.0d && verticalRadius == 20.0d)
            return MathHelper.squaredHypot(deltaX, deltaZ) < SNIFF_RANGE_SQUARED && deltaY < SNIFF_RANGE_VERTICAL;

        // Immediate retaliation:
        if(horizontalRadius == 5.0d && verticalRadius == 5.0d)
            return MathHelper.squaredHypot(deltaX, deltaZ) < (4.0d) && deltaY < 2.0d;

        // Sonic booms:
        if(horizontalRadius == 15.0d && verticalRadius == 20.0d && entity instanceof PlayerEntity) {
            if(this.getLastAttackTime() < this.age - BOOM_COOLDOWN_AFTER_ATTACK) return false;
            if(this.getMoveControl().isMoving() && entity.getPos().y < this.getY() + 4.0) return false;
            return MathHelper.squaredHypot(deltaX, deltaZ) < (horizontalRadius * horizontalRadius) && deltaY < verticalRadius;
        }

        // Anything else:
        return MathHelper.squaredHypot(deltaX, deltaZ) < (horizontalRadius * horizontalRadius) && deltaY < verticalRadius;
    }

    /**
     *  - WARDENS ARE IMMUNE TO WEAK ARROWS
     *  This allows us to nerf their ranged attacks and target tracking to make it a lot
     *  more dangerous and interesting to fight one of these things.
     */
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean hasReceivedDamage = false, mightReceiveDamage = true;
        Entity attacker = source.getAttacker();
        if (source.getSource() instanceof PersistentProjectileEntity && !(source.getSource() instanceof TridentEntity)) {
            if(attacker == null || !(attacker instanceof PlayerEntity)) {
                mightReceiveDamage = false;
            }
            else {
                double distanceSquared = attacker.getPos().squaredDistanceTo(this.getPos());
                if(distanceSquared > 256.0d) mightReceiveDamage = false;
                else amount = (amount * (256.0f - (float)attacker.getPos().squaredDistanceTo(this.getPos())))/256.0f;
            }
            if(amount < 3.0f) mightReceiveDamage = false;
        }
        if(mightReceiveDamage) hasReceivedDamage = super.damage(world, source, amount);

        if (!(this.getWorld().isClient || this.isAiDisabled() || this.isDiggingOrEmerging())) {
            this.increaseAngerAt(attacker, Angriness.ANGRY.getThreshold() + 20, false);
            if (this.brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && attacker instanceof LivingEntity livingEntity) {
                if (source.isDirect() || this.isInRange(livingEntity, 5.0)) {
                    this.updateAttackTarget(livingEntity);
                }
            }
        }
        return hasReceivedDamage;
    }

}
