package frootloops.versus.mod.mobs.hostile.end;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EndermanHideAndWaitGoal<T extends LivingEntity> extends Goal {

    private static final Identifier FOLLOW_RANGE_MODIFIER_ID = Identifier.ofVanilla("enderman_waiting");
    private static final EntityAttributeModifier FOLLOW_RANGE_BONUS = new EntityAttributeModifier(FOLLOW_RANGE_MODIFIER_ID, 64.0, EntityAttributeModifier.Operation.ADD_VALUE);

    protected final EndermanEntity mob;
    protected final EntityNavigation mobNavigation;
    @Nullable
    protected LivingEntity targetEntity;


    public EndermanHideAndWaitGoal(EndermanEntity mob) {
        this.mob = mob;
        this.mobNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Control.MOVE));
    }


    @Override
    public boolean canStart() {
        if(mob.getEntityWorld().isClient()) return false;

        targetEntity = mob.getAttacker();
        if (targetEntity == null) return false;
        if (targetEntity.isTouchingWater()) {
            for(int i = 0; i < 3; i++) {
                if(this.teleportAway()) return true;
            }
        }
        if (targetEntity.squaredDistanceTo(mob) < 16.0) return false;
        if (this.mob.getNavigation().findPathTo(targetEntity, 0) != null) return false;
        for(int i = 0; i < 3; i++) {
            if(this.teleportAway()) return true;
        }
        return false;
    }

    @Override
    public void start() {
        EntityAttributeInstance entityAttributeInstance = mob.getAttributeInstance(EntityAttributes.FOLLOW_RANGE);
        entityAttributeInstance.removeModifier(FOLLOW_RANGE_MODIFIER_ID);
        entityAttributeInstance.addTemporaryModifier(FOLLOW_RANGE_BONUS);
    }

    @Override
    public boolean shouldContinue() {
        if(!targetEntity.isAlive()) return false;
        if(targetEntity.isTouchingWater()) return true;

        double squaredDistance = targetEntity.squaredDistanceTo(mob);
        if(squaredDistance < 9.0 || squaredDistance > 400.0) return false;
        if(squaredDistance < 64.0) return this.mob.getNavigation().findPathTo(targetEntity, 0) == null;
        return Combat.isLookingTowardsEntity(targetEntity, mob, false);
    }

    @Override
    public void tick() {
        if(mob.timeUntilRegen == 20) {
            LivingEntity attacker = (LivingEntity) mob.getRecentDamageSource().getAttacker();
            if (attacker != null && attacker.isTouchingWater()) this.targetEntity = attacker;
            this.teleportAway();
        }
    }

    @Override
    public void stop() {
        if(!targetEntity.isAlive()) return;
        if(mob.squaredDistanceTo(targetEntity) > 144.0) this.teleportBackToPlayer();
        mob.setTarget(targetEntity);
    }


    private boolean teleportAway() {
        if (!mob.getEntityWorld().isClient() && mob.isAlive()) {
            double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * 64.0;
            double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * 64.0;
            double y = mob.getY() + (double)Math.min(mob.getEntityWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, (int)x, (int)z) + 16, (mob.getRandom().nextInt(64) - 16));
            return teleportTo(x, y, z);
        }
        return false;
    }

    private boolean teleportBackToPlayer() {
        if (!mob.getEntityWorld().isClient() && mob.isAlive()) {
            //Vec3d vec3d = new Vec3d(mob.getX() - targetEntity.getX(), mob.getBodyY(0.5) - targetEntity.getEyeY(), mob.getZ() - targetEntity.getZ());
            //vec3d = vec3d.normalize();
            //double range = 16.0;
            double x = targetEntity.getX() + (mob.getRandom().nextDouble() - 0.5) * 16.0;// - vec3d.x * range;
            double y = targetEntity.getY() + (double)(mob.getRandom().nextInt(16) - 8);// - vec3d.y * range;
            double z = targetEntity.getZ() + (mob.getRandom().nextDouble() - 0.5) * 16.0;// - vec3d.z * range;
            return teleportTo(x, y, z);
        }
        return false;
    }

    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        BlockState blockState = mob.getEntityWorld().getBlockState(mutable);
        if(!blockState.getFluidState().isEmpty()) return false;

        while(mutable.getY() > mob.getEntityWorld().getBottomY() && !blockState.isOpaqueFullCube()) {
            mutable.move(Direction.DOWN);
            blockState = mob.getEntityWorld().getBlockState(mutable);
            if(!blockState.getFluidState().isEmpty()) return false;
        }

        blockState = mob.getEntityWorld().getBlockState(mutable);
        boolean isBlockPermitted = blockState.isOpaqueFullCube();
        boolean isBlockWet = blockState.getFluidState().isIn(FluidTags.WATER);
        if (isBlockPermitted && !isBlockWet) {
            Vec3d vec3d = mob.getEntityPos();
            //VersusMod.MOD_LOGGER.warn("    > " + mob.getEntityWorld().getTime() + ": Teleport attempt to " + mutable);
            if (mob.teleport(x, y, z, true)) {
                mob.getEntityWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(mob));
                if (!mob.isSilent()) {
                    mob.getEntityWorld().playSound(null, mob.lastX, mob.lastY, mob.lastZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, mob.getSoundCategory(), 1.0F, 1.0F);
                    mob.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                return true;
            }
            return false;
        }
        else {
            return false;
        }
    }
}
