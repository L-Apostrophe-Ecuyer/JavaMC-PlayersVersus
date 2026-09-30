package frootloops.versus.mod.mobs.hostile.end;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class EndermanHideAndWaitGoal<T extends LivingEntity> extends Goal {

    private static final Identifier FOLLOW_RANGE_MODIFIER_ID = Identifier.withDefaultNamespace("enderman_waiting");
    private static final AttributeModifier FOLLOW_RANGE_BONUS = new AttributeModifier(FOLLOW_RANGE_MODIFIER_ID, 64.0, AttributeModifier.Operation.ADD_VALUE);

    protected final Enderman mob;
    protected final PathNavigation mobNavigation;
    @Nullable
    protected LivingEntity targetEntity;


    public EndermanHideAndWaitGoal(Enderman mob) {
        this.mob = mob;
        this.mobNavigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }


    @Override
    public boolean canUse() {
        if(mob.level().isClientSide()) return false;

        targetEntity = mob.getLastHurtByMob();
        if (targetEntity == null) return false;
        if (targetEntity.isInWater()) {
            for(int i = 0; i < 3; i++) {
                if(this.teleportAway()) return true;
            }
        }
        if (targetEntity.distanceToSqr(mob) < 16.0) return false;
        if (this.mob.getNavigation().createPath(targetEntity, 0) != null) return false;
        for(int i = 0; i < 3; i++) {
            if(this.teleportAway()) return true;
        }
        return false;
    }

    @Override
    public void start() {
        AttributeInstance entityAttributeInstance = mob.getAttribute(Attributes.FOLLOW_RANGE);
        entityAttributeInstance.removeModifier(FOLLOW_RANGE_MODIFIER_ID);
        entityAttributeInstance.addTransientModifier(FOLLOW_RANGE_BONUS);
    }

    @Override
    public boolean canContinueToUse() {
        if(!targetEntity.isAlive()) return false;
        if(targetEntity.isInWater()) return true;

        double squaredDistance = targetEntity.distanceToSqr(mob);
        if(squaredDistance < 9.0 || squaredDistance > 400.0) return false;
        if(squaredDistance < 64.0) return this.mob.getNavigation().createPath(targetEntity, 0) == null;
        return Combat.isLookingTowardsEntity(targetEntity, mob, false);
    }

    @Override
    public void tick() {
        if(mob.getInvulnerableTime() == 20) {
            LivingEntity attacker = (LivingEntity) mob.getLastDamageSource().getEntity();
            if (attacker != null && attacker.isInWater()) this.targetEntity = attacker;
            this.teleportAway();
        }
    }

    @Override
    public void stop() {
        if(!targetEntity.isAlive()) return;
        if(mob.distanceToSqr(targetEntity) > 144.0) this.teleportBackToPlayer();
        mob.setTarget(targetEntity);
    }


    private boolean teleportAway() {
        if (!mob.level().isClientSide() && mob.isAlive()) {
            double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * 64.0;
            double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * 64.0;
            double y = mob.getY() + (double)Math.min(mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int)x, (int)z) + 16, (mob.getRandom().nextInt(64) - 16));
            return teleportTo(x, y, z);
        }
        return false;
    }

    private boolean teleportBackToPlayer() {
        if (!mob.level().isClientSide() && mob.isAlive()) {
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
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, y, z);
        BlockState blockState = mob.level().getBlockState(mutable);
        if(!blockState.getFluidState().isEmpty()) return false;

        while(mutable.getY() > mob.level().getMinY() && !blockState.isSolidRender()) {
            mutable.move(Direction.DOWN);
            blockState = mob.level().getBlockState(mutable);
            if(!blockState.getFluidState().isEmpty()) return false;
        }

        blockState = mob.level().getBlockState(mutable);
        boolean isBlockPermitted = blockState.isSolidRender();
        boolean isBlockWet = blockState.getFluidState().is(FluidTags.WATER);
        if (isBlockPermitted && !isBlockWet) {
            Vec3 vec3d = mob.position();
            //VersusMod.MOD_LOGGER.warn("    > " + mob.getEntityWorld().getTime() + ": Teleport attempt to " + mutable);
            if (mob.randomTeleport(x, y, z, true, state -> false)) {
                mob.level().gameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Context.of(mob));
                if (!mob.isSilent()) {
                    mob.level().playSound(null, mob.xo, mob.yo, mob.zo, SoundEvents.ENDERMAN_TELEPORT, mob.getSoundSource(), 1.0F, 1.0F);
                    mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
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
