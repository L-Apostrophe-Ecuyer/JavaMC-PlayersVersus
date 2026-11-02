package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.WorldTime;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.mobs.hostile.end.EndermanHideAndWaitGoal;
import frootloops.versus.mod.mobs.hostile.end.EndermanShulkerCarrier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enderman.class)
public abstract class EndermanMixin extends Monster implements NeutralMob {

    private int angerTime = 0;
    protected EndermanMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    private boolean teleport(double x, double y, double z) {return false;}

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        AttributeInstance instanceKnockback = this.getAttributes().getInstance(Attributes.KNOCKBACK_RESISTANCE);
        if (instanceKnockback != null) instanceKnockback.setBaseValue(0.6D);
        // Now and then, a shulker in its arms instead of a block (EndermanShulkerCarrier)
        EndermanShulkerCarrier.maybeGiveShulker(world, (Enderman) (Object) this, difficulty, spawnReason, this.random);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    /**
     * Carrying a shulker, it's as wide as the shulker (near enough: a sliver of the shulker stands out, to be hit), which
     * it holds inside its box: wherever it can stand or teleport to, the shulker isn't in a wall.
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        if (!EndermanShulkerCarrier.carries(this)) return dimensions;
        return EntityDimensions.scalable(VersusSettings.Endermen.SHULKER_CARRIER_WIDTH, dimensions.height()).withEyeHeight(dimensions.eyeHeight());
    }

    /** Its size follows what it carries: a shulker taken up or let go, it refreshes. */
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.refreshDimensions();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.refreshDimensions();
    }

    /** A shulker it carries is held at its chest, in its arms (drawn before it: client ShulkerRendererCarriedMixin), not on its head. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        if (passenger instanceof Shulker) return new Vec3(0.0, VersusSettings.Endermen.SHULKER_HELD_AT * scale, 0.0);
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
    }

    /** The enderman goes where it means to: a shulker in its arms doesn't steer it. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return EndermanShulkerCarrier.carries(this) ? null : super.getControllingPassenger();
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void addWaitForPlayerGoal(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new EndermanHideAndWaitGoal((Enderman) ((Object)this)));
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor worldAccess, EntitySpawnReason spawnReason) {
        if(!super.checkSpawnRules(worldAccess, spawnReason)) return false;
        if(spawnReason == EntitySpawnReason.NATURAL && worldAccess.dimensionType().hasSkyLight()) {
            if(worldAccess instanceof Level world && world.isRaining() && !world.isThundering()) { // Endermen more common when foggy or during new moons
                return true;
            }
            if((WorldTime.moonPhase(worldAccess) + 2) % 8 < 6 && this.random.nextInt(4) < 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int calculateFallDamage(double fallDistance, float damagePerDistance) {
        return super.calculateFallDamage(fallDistance - 4.0f, damagePerDistance) - 5;
    }


    @Overwrite
    public boolean isBeingStaredBy(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Blocks.CARVED_PUMPKIN.asItem())) return false;
        double squaredDistance = this.distanceToSqr(player);

        // Targeted players will be attacked if returns false. We make it so endermen only attack when you're looking, making chases more panicky.
        if(this.getTarget() == player) {
            // 26.3 keeps the game time the anger ends at instead of the ticks left.
            if(this.getPersistentAngerEndTime() - this.level().getGameTime() < 10) return false;
            if(this.hurtTime > 0 && this.lastHurt > 5.0f) return true;
            if(squaredDistance > 64.0) return false;
            else if(squaredDistance > 16.0) return !(Combat.isLookingTowards(player, this.getEyePosition(), -0.3));
            else return false;
        }
        // Untargeted players, Endermen will teleport up to them until they're in range for aggro:
        else {
            if (player.yHeadRotO != player.yHeadRot) return false;
            else if(squaredDistance > 8192.0) return false;

            Vec3 playerRotationVect = player.getViewVector(1.0f).normalize();
            Vec3 directionVect = new Vec3(this.getX() - player.getX(), this.getY(0.6) - player.getEyeY(), this.getZ() - player.getZ());
            double distance = directionVect.length();
            double dotProduct = (playerRotationVect).dot(directionVect.scale(1.0/distance));
            double dotProductThreshold = distance < 10.0 ? 0.95 : 1.0 - 0.05 / (distance/2 - 4.5);
            if (dotProduct > dotProductThreshold) {
                boolean hasLineOfSight = player.hasLineOfSight(this);
                if(hasLineOfSight & angerTime == 0) this.lookAt(player, 200f, 200f);
                if (squaredDistance > 256.0 && hasLineOfSight) {
                    angerTime = 0;
                    Vec3 target = this.position().add(directionVect.scale(-0.65));
                    teleport(target.x + (this.random.nextDouble() - 0.5) * 4.0, target.y + (double) this.random.nextInt(16) - 8.0, target.z + (this.random.nextDouble() - 0.5) * 4.0);
                    this.playAmbientSound();
                    return false;
                }
                else if(!hasLineOfSight) {
                    angerTime = Math.max(0, angerTime - 8);
                }
                else {
                    angerTime += distance < 8.0 ? 16 : 8;
                    if(angerTime > 64) {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    boolean teleportToEntity(Entity entity, double distance) {
        Vec3 direction = new Vec3(this.getX() - entity.getX(), this.getY(0.5) - entity.getEyeY(), this.getZ() - entity.getZ());
        direction = direction.normalize();
        double e = this.getX() + (this.random.nextDouble() - 0.5) * 4.0 - direction.x * distance;
        double f = this.getY() + (double)(this.random.nextInt(16) - 8) - direction.y * distance;
        double g = this.getZ() + (this.random.nextDouble() - 0.5) * 4.0 - direction.z * distance;
        return this.teleport(e, f, g);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            volume *= 0.8f;
            pitch *= 0.75f;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
        }
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void darknessWhenAngered(@Nullable LivingEntity target, CallbackInfo ci) {
        if(target != null) target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, false, false));
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }
}
