package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;


@Mixin(Warden.class)
public class WardenMixin extends Monster {

    private static final int BOOM_COOLDOWN_AFTER_ATTACK = 240;
    private static final double SNIFF_RANGE_SQUARED = 36.0d, SNIFF_RANGE_VERTICAL = 8.0d;
    private static final double BOOM_RANGE_HORIZONTAL = 8.0d, BOOM_RANGE_VERTICAL = 10.0d,
            BOOM_RANGE_HORIZONTAL_SQUARED = BOOM_RANGE_HORIZONTAL * BOOM_RANGE_HORIZONTAL;

    protected WardenMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }


    @Shadow
    @Contract("null->false")
    public boolean canTargetEntity(@Nullable Entity entity) {
        if (entity instanceof LivingEntity livingEntity
                && this.level() == entity.level()
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                && !this.isAlliedTo(entity)
                && livingEntity.getType() != EntityType.ARMOR_STAND
                && livingEntity.getType() != EntityType.WARDEN
                && !livingEntity.isInvulnerable()
                && !livingEntity.isDeadOrDying()) {
            return true;
        }
        return false;
    }

    @Shadow
    private boolean isDiggingOrEmerging() {return false;}

    @Shadow
    public void setAttackTarget(LivingEntity target) {}


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
    private AngerManagement angerManagement;

    @Shadow
    public AngerLevel getAngerLevel() {
        return AngerLevel.byAnger(this.getActiveAnger());
    }

    @Shadow
    private int getActiveAnger() {
        return this.angerManagement.getActiveAnger(this.getTarget());
    }

    /**
     * Darkness' fog amount depends on how close the player is
     */
    @Overwrite
    public static void applyDarknessAround(ServerLevel world, Vec3 pos, @Nullable Entity entity, int range) {
        if(entity instanceof Warden warden && warden.getTarget() instanceof Player player && player.distanceToSqr(warden) < 256.0) {
            MobEffectInstance intenseDarkness = new MobEffectInstance(MobEffects.DARKNESS, 120, 2, false, false);
            MobEffectUtil.addEffectToPlayersAround(world, entity, pos, 16.0, intenseDarkness, 200);
        }
        MobEffectInstance mildDarkness = new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false);
        MobEffectUtil.addEffectToPlayersAround(world, entity, pos, 32.0, mildDarkness, 200);
    }

    /**
    This allows players to sneak away from Wardens, and Wardens to lose their scent, making for tense encounters.
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void reduceAngerTowardsSneakyPlayers(CallbackInfo ci){
        if(this.getAngerLevel() == AngerLevel.ANGRY && this.tickCount > 200 && this.tickCount % 3 == 0) {
            Entity target = this.getTarget();
            if(target != null && (target.isDiscrete() || target.distanceToSqr(this.position()) > 400)) {
                this.angerManagement.increaseAnger(target, -1);
            }
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void dontDespawnWhenSummonedByCheats(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData, CallbackInfoReturnable cir){
        if (spawnReason != EntitySpawnReason.TRIGGERED && spawnReason != EntitySpawnReason.SPAWNER) {
            this.setHealth(300.0f); // Bit easier to kill compared to regular shrieker Wardens
            this.setPersistenceRequired();
        }
        else if(spawnReason == EntitySpawnReason.TRIGGERED) {
            Player closestPlayer = this.level().getNearestPlayer(this, 64.0d);
            if(closestPlayer != null && !this.closerThan(closestPlayer, 16.0)) {
                WardenAi.setDisturbanceLocation((Warden) ((Object)this), closestPlayer.blockPosition());
                this.angerManagement.increaseAnger(closestPlayer, closestPlayer.isSprinting() ? 70 : 50);
            }
        }
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        if(spawnReason != EntitySpawnReason.TRIGGERED && this.blockPosition().getY() > -32) return false;
        else return super.checkSpawnRules(world, spawnReason);
    }

    /**
     *  - ANGER TOWARDS PLAYERS REDUCED
     * The anger amount from hearing or sniffing players is now mostly reduced, and *depends on distance*.
     * This is to rebalance the fact that Wardens have a much wider range for hearing vibrations.
     */
    @Overwrite
    public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {
        if(amount < 1) return;
        if (!this.isNoAi() && this.canTargetEntity(entity)) {
            boolean isPlayer = entity instanceof Player;

            // Reset dig cooldown:
            if (this.getBrain().hasMemoryValue(MemoryModuleType.DIG_COOLDOWN)) {
                this.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, isPlayer ? 1200L : 600L);
            }

            // If Player:
            if(isPlayer) {

                // If current target isnt a player, but new one is, forget them and prioritize player:
                if(this.getTarget() != null && this.getTarget().isAlive() && !(this.getTarget() instanceof Player)) {
                    this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    this.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
                    Optional<LivingEntity> suspect = this.angerManagement.getActiveEntity();
                    while(suspect.isPresent()) {
                        if(suspect.get() instanceof Player) break;
                        else angerManagement.clearAnger(suspect.get());
                        suspect = this.angerManagement.getActiveEntity();
                    }
                }

                // Get rebalanced anger amount, for regular sniffing or hearing:
                else if(amount == 10 || amount == 35) {
                    if(entity.isSprinting()) amount = 40;
                    else if(!listening) amount = 5;
                    else {
                        double squaredDistance = this.distanceToSqr(entity);
                        if(squaredDistance > 24.0) amount /= 5;
                        else if (squaredDistance > 16.0) amount /= 2;
                        else if (squaredDistance < 4.0) amount = 3 + amount/2;
                    }
                }
            }
            else {
                // If current target IS a player, but new one isn't, ignore them:
                if(this.getTarget() != null && this.getTarget().isAlive() && (this.getTarget() instanceof Player player) && player.distanceToSqr(this) < 1024.0) {
                    return;
                }
            }

            // Increase anger:
            this.angerManagement.increaseAnger(entity, amount);

            // Play sound:
            if (listening && !this.hasPose(Pose.ROARING)) {
                this.playSound(this.getAngerLevel().getListeningSound(), 10.0F, this.getVoicePitch());
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
    public boolean closerThan(Entity entity, double horizontalRadius, double verticalRadius) {
        double deltaX = entity.getX() - this.getX();
        double deltaY = entity.getY() - this.getY();
        double deltaZ = entity.getZ() - this.getZ();

        // Sniffing:
        if(horizontalRadius == 6.0d && verticalRadius == 20.0d)
            return Mth.lengthSquared(deltaX, deltaZ) < SNIFF_RANGE_SQUARED && deltaY < SNIFF_RANGE_VERTICAL;

        // Immediate retaliation:
        if(horizontalRadius == 5.0d && verticalRadius == 5.0d)
            return Mth.lengthSquared(deltaX, deltaZ) < (4.0d) && deltaY < 2.0d;

        // Sonic booms:
        if(horizontalRadius == 15.0d && verticalRadius == 20.0d && entity instanceof Player) {
            if(this.getLastHurtMobTimestamp() < this.tickCount - BOOM_COOLDOWN_AFTER_ATTACK) return false;
            if(this.getMoveControl().hasWanted() && entity.position().y < this.getY() + 4.0) return false;
            return Mth.lengthSquared(deltaX, deltaZ) < (horizontalRadius * horizontalRadius) && deltaY < verticalRadius;
        }

        // Anything else:
        return Mth.lengthSquared(deltaX, deltaZ) < (horizontalRadius * horizontalRadius) && deltaY < verticalRadius;
    }

    /**
     *  - WARDENS ARE IMMUNE TO WEAK ARROWS
     *  This allows us to nerf their ranged attacks and target tracking to make it a lot
     *  more dangerous and interesting to fight one of these things.
     */
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean hasReceivedDamage = false, mightReceiveDamage = true;
        Entity attacker = source.getEntity();
        if (source.getDirectEntity() instanceof AbstractArrow && !(source.getDirectEntity() instanceof ThrownTrident)) {
            if(attacker == null || !(attacker instanceof Player)) {
                mightReceiveDamage = false;
            }
            else {
                double distanceSquared = attacker.position().distanceToSqr(this.position());
                if(distanceSquared > 256.0d) mightReceiveDamage = false;
                else amount = (amount * (256.0f - (float)attacker.position().distanceToSqr(this.position())))/256.0f;
            }
            if(amount < 3.0f) mightReceiveDamage = false;
        }
        if(mightReceiveDamage) hasReceivedDamage = super.hurtServer(world, source, amount);

        if (!(this.level().isClientSide() || this.isNoAi() || this.isDiggingOrEmerging())) {
            this.increaseAngerAt(attacker, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
            if (this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && attacker instanceof LivingEntity livingEntity) {
                if (source.isDirect() || this.closerThan(livingEntity, 5.0)) {
                    this.setAttackTarget(livingEntity);
                }
            }
        }
        return hasReceivedDamage;
    }

}
