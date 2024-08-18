package frootloops.versus.mod;


import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.LivingEntityAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public abstract class Combat {

    public static final Identifier ATTACK_REACH_MODIFIER_ID = Identifier.of("base_attack_reach");

    public static final double MIN_COOLDOWN_TO_SWING = 0.8d;

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 0.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 2.5d;
    public static final double PLAYER_MAX_ATTACK_SPEED = 2.5d;


    public static void onInitialize() {
    }

    private static double getCappedAttackSpeedOf(PlayerEntity player) {
        return Math.min(PLAYER_MAX_ATTACK_SPEED, player.getAttributeValue(EntityAttributes.ATTACK_SPEED));
    }

    public static int getTicksPerAttackOf(PlayerEntity player) {
        return (int)(20d / Combat.getCappedAttackSpeedOf(player));
    }

    public static double getAttackChargeProgress(PlayerEntity player) {
        int lastAttackTicks = ((LivingEntityAccessor)player).getLastAttackedTicks();
        double attackSpeed = Combat.getCappedAttackSpeedOf(player);
        return (attackSpeed * (double)lastAttackTicks) / 20.0d;
    }

    public static double getAttackRangeBonusOf(ItemStack itemStack) {
        if(itemStack == null || itemStack.isEmpty() || !itemStack.isDamageable()) return 0.0d;
        Item item = itemStack.getItem();
        if(item instanceof TridentItem) return 1.0d;
        if(item instanceof HoeItem) return 1.0d;
        if(item instanceof SwordItem) return 0.5d;
        return 0.0d;
    }

    public static double getAttackRange(PlayerEntity player, double attackChargeProgress) {
        double reachAttributeValue = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        attackChargeProgress = Math.min(1.0d, attackChargeProgress - 0.5d);
        double chargeTimeBonus = attackChargeProgress * attackChargeProgress;
        double ridingBonus = player.hasVehicle() && player.getVehicle().isAlive() ? 0.5d : 0d;
        return reachAttributeValue + chargeTimeBonus + ridingBonus;
    }

    public static double getAttackRange(PlayerEntity player) {
        return Combat.getAttackRange(player, Combat.getAttackChargeProgress(player));
    }

    public static boolean isInAttackRangeOf(PlayerEntity player, Entity entity, double attackCooldownProgress) {
        double range = Combat.getAttackRange(player, attackCooldownProgress);
        return (range * range) > player.getEyePos().squaredDistanceTo(entity.getEyePos());
    }

    public static void doSpecialSweepAttack(PlayerEntity player, double attackRange, int level) {
        if(level < 1) return;

        World world = player.getWorld();
        player.spawnSweepAttackParticles();
        player.swingHand(player.getActiveHand());
        //player.getItemCooldownManager().set(player.getActiveItem().getItem(), 19 - level * 2);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(), 1.0F, 1.0F);

        VersusMod.MOD_LOGGER.warn("SWEEP ATTACK: With range " + attackRange);

        attackRange = attackRange - (0.5d * (double)(3 - level));
        double attackRangeSquared = attackRange * attackRange;

        // Attack entities:
        Vec3d playerPos = player.getEyePos();
        Box boundingBox = new Box(playerPos.x - attackRange, playerPos.y - attackRange, playerPos.z - attackRange, playerPos.x + attackRange, playerPos.y + attackRange, playerPos.z + attackRange);
        List<LivingEntity> entitiesInRange = world.getEntitiesByClass(LivingEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
        for (LivingEntity targetEntity : entitiesInRange) {
            if(!targetEntity.isTeammate(player) && targetEntity != player) {

                VersusMod.MOD_LOGGER.warn("    -> Entity " + targetEntity.getName().getString() + " is in range? " + targetEntity.getEyePos().squaredDistanceTo(playerPos) + " < " + attackRangeSquared + "? " + (targetEntity.getEyePos().squaredDistanceTo(playerPos) < attackRangeSquared));
                if(targetEntity.squaredDistanceTo(playerPos) < attackRangeSquared) VersusMod.MOD_LOGGER.warn("       Entity " + targetEntity.getName().getString() + " is in sight? " + Combat.isLookingTowards(player, targetEntity.getPos()));

                if (targetEntity.getEyePos().squaredDistanceTo(playerPos) - 2.0 < attackRangeSquared && Combat.isLookingTowards(player, targetEntity.getPos())) {
                    if(player.canSee(targetEntity)) {

                        VersusMod.MOD_LOGGER.warn("       * Entity Attacked!!!");

                        player.setSprinting(true);
                        ((LivingEntityAccessor) player).setLastAttackedTicks(8 + 2 * level);
                        player.attack(targetEntity);
                    }
                }
            }
        }

        // Break foliage:
        if(player.getActiveItem().getItem() instanceof HoeItem) {
            Vec3d hitPos = Combat.getHitResultOf(player, attackRange - 1d).getPos();
            BlockPos blockPosOfHit = new BlockPos((int) hitPos.x, (int) hitPos.y, (int) hitPos.z);
            BlockPos blockPos, above;
            BlockState blockState;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = -1; y <= 1; y++) {
                        blockPos = blockPosOfHit.add(x, y, z);
                        blockState = world.getBlockState(blockPos);
                        if (blockState.getHardness(world, blockPos) == 0.0) {
                            world.breakBlock(blockPos, true, player);
                            above = blockPos.add(0, 1, 0);
                            if (world.getBlockState(above).getHardness(world, above) == 0.0F) {
                                world.breakBlock(above, true, player);
                            }
                        }
                    }
                }
            }
        }
    }

    public static final boolean isLookingTowardsEntity(LivingEntity looker, LivingEntity target, boolean strict){
        return Combat.isLookingTowards(looker,new Vec3d(target.getX(), target.getEyeY(), target.getZ()),strict);
    }

    public static final boolean isLookingTowardsEntity(LivingEntity looker, LivingEntity target, double dotProductThreshold){
        return Combat.isLookingTowards(looker,new Vec3d(target.getX(), target.getEyeY(), target.getZ()),dotProductThreshold);
    }


    public static final boolean isLookingTowards(LivingEntity looker, Vec3d targetPos){
        return Combat.isLookingTowards(looker,targetPos,false);
    }

    public static final boolean isLookingTowards(LivingEntity looker, Vec3d targetPos, boolean strict){
        return Combat.isLookingTowards(looker,targetPos,strict ? -0.75 : -0.5);
    }

    public static final boolean isLookingTowards(LivingEntity looker, Vec3d targetPos, double dotProductThreshold){
        if(looker==null || targetPos == null) return false;
        Vec3d rotationVector = looker.getRotationVec(1.0F);
        Vec3d positionVector = targetPos.relativize(looker.getEyePos());
        if(rotationVector.dotProduct(positionVector) >= 0.0F) return false;
        else return (rotationVector.dotProduct(positionVector.normalize()) < dotProductThreshold);
    }

    public static HitResult getHitResultOf(LivingEntity entity, double range) {
        return getHitResultOf(entity.getYaw(1F), entity.getPitch(1f), range, entity);
    }

    public static HitResult getHitResultOf(float yaw, float pitch, double range, Entity entity) {

        // Convert degrees to radians manually
        double yawRadians = yaw * Math.PI / 180.0;
        double pitchRadians = pitch * Math.PI / 180.0;

        // Calculate the components of the direction vector
        double dx = range * -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double dy = range * -Math.sin(pitchRadians);
        double dz = range * Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vec3d direction = new Vec3d(dx, dy, dz);

        World world = entity.getWorld();
        Vec3d posStart = entity.getEyePos();
        Vec3d posStop = posStart.add(direction);

        HitResult hitResult = world.raycast(new RaycastContext(posStart, posStop, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
        if (((HitResult)hitResult).getType() != HitResult.Type.MISS) posStop = ((HitResult)hitResult).getPos();

        HitResult entityHitResult = ProjectileUtil.getEntityCollision(world, entity, posStart, posStop, entity.getBoundingBox().stretch(direction).expand(1.0), EntityPredicates.CAN_COLLIDE.and(e -> e != null));
        if (entityHitResult != null) hitResult = entityHitResult;

        return (HitResult)hitResult;
    }

    public static Box getMobAttackBox(MobEntity mob, boolean jump) {
        Entity ridingEntity = mob.getVehicle();
        Box attackBox;
        if (ridingEntity != null) {
            Box box = ridingEntity.getBoundingBox();
            Box box2 = mob.getBoundingBox();
            attackBox = new Box(Math.min(box2.minX, box.minX), box2.minY, Math.min(box2.minZ, box.minZ), Math.max(box2.maxX, box.maxX), box2.maxY, Math.max(box2.maxZ, box.maxZ));
        }
        else if (mob instanceof EndermanEntity) {
            attackBox = mob.getBoundingBox().expand(0.5d, mob.getEyeHeight(mob.getPose())/2 + 1d, 0.5d);
        }
        else if (jump || !mob.isOnGround()) {
            attackBox = mob.getBoundingBox().expand(0d, mob.getEyeHeight(mob.getPose())/2 + 0.5d, 0d);
        }
        else {
            attackBox = mob.getBoundingBox().offset(0d, mob.getEyeHeight(mob.getPose())/2, 0d);
        }
        double attackRangeBonus = Combat.getAttackRangeBonusOf(mob.getEquippedStack(EquipmentSlot.MAINHAND));
        return attackBox.expand(0.8 + attackRangeBonus, attackRangeBonus/2, 0.8 + attackRangeBonus);
    }

    public static Box getEntityHitbox(Entity entity) {
        Box box = entity.getBoundingBox();
        Entity ridingEntity = entity.getVehicle();
        if (ridingEntity != null) {
            Vec3d vec3d = ridingEntity.getPassengerRidingPos(entity);
            return box.withMinY(Math.max(vec3d.y, box.minY));
        } else {
            return box;
        }
    }

    public static boolean canPlayerSprint(PlayerEntity player) {
        return canPlayerSprint(player.getHungerManager());
    }

    public static boolean canPlayerSprint( HungerManager hungerManager) {
        if(!VersusSettings.DO_FOOD_OVERHAUL) return hungerManager.getFoodLevel() > 6;
        if(hungerManager.getFoodLevel() != 0) return true;
        if(hungerManager.getSaturationLevel() > 0.0f) return true;
        return false;
    }
}