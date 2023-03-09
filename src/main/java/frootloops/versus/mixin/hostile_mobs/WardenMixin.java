package frootloops.versus.mixin.hostile_mobs;

import com.google.common.annotations.VisibleForTesting;
import frootloops.versus.Main;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.WardenAngerManager;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angriness;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(WardenEntity.class)
public class WardenMixin extends HostileEntity {


    /**
     *  - WARDENS ARE IMMUNE TO WEAK ARROWS
     *  The only buff Wardens get. This allows us to nerf their ranged attacks and target
     *  tracking to make it a lot more dangerous and interesting to fight one of these things.
     */
    @Shadow @VisibleForTesting
    public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {}

    @Shadow
    private boolean isDiggingOrEmerging() {return false;}

    @Shadow
    public void updateAttackTarget(LivingEntity target) {}

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hasRecievedDamage = false, mightRecieveDamage = true;
        if (source.getSource() instanceof PersistentProjectileEntity && !(source.getSource() instanceof TridentEntity)) {
            Entity attacker = source.getAttacker();
            if(attacker == null || !(attacker instanceof PlayerEntity)) {
                mightRecieveDamage = false;
            }
            else {
                double distanceSquared = attacker.getPos().squaredDistanceTo(this.getPos());
                if(distanceSquared > 256.0d) mightRecieveDamage = false;
                else amount = (amount * (256.0f - (float)attacker.getPos().squaredDistanceTo(this.getPos())))/256.0f;
            }
            if(amount < 3.0f) mightRecieveDamage = false;
        }
        if(mightRecieveDamage) hasRecievedDamage = super.damage(source, amount);

        if (!(this.world.isClient || this.isAiDisabled() || this.isDiggingOrEmerging())) {
            Entity entity = source.getAttacker();
            this.increaseAngerAt(entity, Angriness.ANGRY.getThreshold() + 20, false);
            if (this.brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                if (!source.isIndirect() || this.isInRange(livingEntity, 5.0)) {
                    this.updateAttackTarget(livingEntity);
                }
            }
        }
        return hasRecievedDamage;
    }



    /**
     *  - ANGER TRACKING NERFED
     * Wardens will quickly lose sight (haha) of their targets, meaning if you run away
     * and crouch or hide again, the Warden won't be able to pinpoint where you are anymore.
     * This is to incentive other play styles, like Tom & Jerry chases, rather than just
     * running away and waiting.
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
        if(this.getAngriness() == Angriness.ANGRY && this.age % 2 == 0) {
            Entity target = this.getTarget();
            if(target != null && target instanceof PlayerEntity && target.isSneaky()) {
                this.angerManager.increaseAngerAt(target, -1);
            }
        }
    }



    /***
     *  - SONIC BOOM NERFED
     * Ranged sonic boom attacks have a shorter range. This allows for closer encounters,
     * fewer frustrating deaths, and things like arrow invulnerability.You want to know
     * why you're being attacked, and have a way to avoiding it, but by default, these
     * sonic booms are simply too punishing.
     */
    private static final double NEW_RANGE_HORIZONTAL = 8.0d, NEW_RANGE_VERTICAL = 12.0d,
            NEW_RANGE_HORIZONTAL_SQUARED = NEW_RANGE_HORIZONTAL * NEW_RANGE_HORIZONTAL;

    protected WardenMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
        double d = entity.getX() - this.getX();
        double e = entity.getY() - this.getY();
        double f = entity.getZ() - this.getZ();

        // Sniffing:
        if(horizontalRadius == 6.0d)
            return MathHelper.squaredHypot(d, f) < (3.0d * 3.0d) && e < 5.0d;

        // Sonic booms:
        if(entity instanceof PlayerEntity)
            return MathHelper.squaredHypot(d, f) < NEW_RANGE_HORIZONTAL_SQUARED && e < NEW_RANGE_VERTICAL;
        else
            return MathHelper.squaredHypot(d, f) < (horizontalRadius * horizontalRadius) && e < verticalRadius;
    }
}
