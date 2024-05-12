package frootloops.versus.mixin.mobs.hostile.overworld;

import com.google.common.annotations.VisibleForTesting;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(WardenEntity.class)
public class WardenMixin extends HostileEntity {


    @Shadow @VisibleForTesting
    public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {}

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

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void reduceAngerTowardsSneakyPlayers(CallbackInfo ci){
        if(this.getAngriness() == Angriness.ANGRY && this.age % 3 == 0) {
            Entity target = this.getTarget();
            if(target != null && (target.isSneaky() || target.squaredDistanceTo(this.getPos()) > 400)) {
                this.angerManager.increaseAngerAt(target, -1);
            }
        }
    }

    @Inject(method = "addDigParticles", at = @At("TAIL"))
    private void moreInvestigative(CallbackInfo ci){
        PlayerEntity closestPlayer = this.method_48926().getClosestPlayer(this, 48.0d);
        if(closestPlayer != null) {
            this.increaseAngerAt(closestPlayer, 20, true);
            WardenBrain.lookAtDisturbance((WardenEntity) ((Object)this), closestPlayer.getBlockPos());
        }
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void dontDespawnWhenSummonedByCheats(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable cir){
        if (spawnReason != SpawnReason.TRIGGERED && spawnReason != SpawnReason.SPAWNER) {
            this.setHealth(300.0f); // Bit easier to kill compared to regular shrieker Wardens
            this.setPersistent();
        }
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.TRIGGERED && this.getBlockPos().getY() > -32) return false;
        else return super.canSpawn(world, spawnReason);
    }


    /***
     *  - SONIC BOOM NERFED
     * Ranged sonic boom attacks have a shorter range. This allows for closer encounters,
     * fewer frustrating deaths, and things like arrow invulnerability.You want to know
     * why you're being attacked, and have a way to avoiding it. In vanilla, these sonic
     * booms are simply too punishing.
     */
    private static final double NEW_RANGE_HORIZONTAL = 8.0d, NEW_RANGE_VERTICAL = 10.0d,
            NEW_RANGE_HORIZONTAL_SQUARED = NEW_RANGE_HORIZONTAL * NEW_RANGE_HORIZONTAL;

    protected WardenMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
        double deltaX = entity.getX() - this.getX();
        double deltaY = entity.getY() - this.getY();
        double deltaZ = entity.getZ() - this.getZ();

        // Sniffing:
        if(horizontalRadius == 6.0d)
            return MathHelper.squaredHypot(deltaX, deltaZ) < (6.0d * 6.0d) && deltaY < 6.0d;

        // Immediate retaliation:
        if(horizontalRadius == 5.0d && verticalRadius == 5.0d)
            return MathHelper.squaredHypot(deltaX, deltaZ) < (4.0d) && deltaY < 2.0d;

        // Sonic booms:
        if(entity instanceof PlayerEntity) return MathHelper.squaredHypot(deltaX, deltaZ) < NEW_RANGE_HORIZONTAL_SQUARED && deltaY < NEW_RANGE_VERTICAL;
        else return MathHelper.squaredHypot(deltaX, deltaZ) < (horizontalRadius * horizontalRadius) && deltaY < verticalRadius;
    }

    /**
     *  - WARDENS ARE IMMUNE TO WEAK ARROWS
     *  This allows us to nerf their ranged attacks and target tracking to make it a lot
     *  more dangerous and interesting to fight one of these things.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
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
        if(mightReceiveDamage) hasReceivedDamage = super.damage(source, amount);

        if (!(this.method_48926().isClient || this.isAiDisabled() || this.isDiggingOrEmerging())) {
            this.increaseAngerAt(attacker, Angriness.ANGRY.getThreshold() + 20, false);
            if (this.brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && attacker instanceof LivingEntity livingEntity) {
                if (!source.isIndirect() || this.isInRange(livingEntity, 5.0)) {
                    this.updateAttackTarget(livingEntity);
                }
            }
        }
        return hasReceivedDamage;
    }
}
