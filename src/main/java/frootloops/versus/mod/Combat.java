package frootloops.versus.mod;


import com.google.common.collect.ImmutableMultimap;
import frootloops.versus.VersusMod;
import frootloops.versus.mixin.players.accessors.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
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

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 2.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 2.5d;
    private static final String[] tools = new String[]{"axe", "sword", "hoe", "pickaxe", "shovel"};
    private static final float[] toolsSpeed  = new float[]{1.0F, 1.6F, 2.0F, 1.2F, 1.4F};
    private static final float[] toolsDamage = new float[]{8.0F, 5.0F, 3.0F, 4.0F, 3.0F};
    private static final String[] toolTiers = new String[]{"wooden", "stone", "golden", "iron", "diamond", "netherite"};
    private static final float[] toolTierDamageBonuses = new float[]{0F, 0F, 1F, 1F, 2F, 3F};

    public static float getAxeSpeedModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[0];}
    public static float getSwordSpeedModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[1];}
    public static float getHoeSpeedModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[2];}
    public static float getPickaxeSpeedModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[3];}
    public static float getShovelSpeedModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[4];}

    public static float getSwordReachModifier() { return (float)PLAYER_BASE_ATTACK_SPEED - toolsSpeed[4];}

    public static void onInitialize() {
        for(int toolIndex = 0; toolIndex < tools.length; toolIndex++) {
            for (int tierIndex = 0; tierIndex < toolTiers.length; tierIndex++) {
                String name = "minecraft:" + toolTiers[tierIndex] + "_" + tools[toolIndex];
                float damage = toolsDamage[toolIndex] + toolTierDamageBonuses[tierIndex] - (float)PLAYER_BASE_ATTACK_DAMAGE;
                float speed = toolsSpeed[toolIndex] - (float)PLAYER_BASE_ATTACK_SPEED;
                setAttributes(name, damage, speed);
            }
        }
        setAttributes("minecraft:trident", 9.0F - (float)PLAYER_BASE_ATTACK_DAMAGE, 1.0F - (float)PLAYER_BASE_ATTACK_SPEED);
    }

    private static void setAttributes(String itemName, float damageModifier, float speedModifier) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> itemBuilder = ImmutableMultimap.builder();
        Item item = Registries.ITEM.get(new Identifier(itemName));
        String modifierType = item instanceof MiningToolItem ? "Tool modifier" : "Weapon modifier";

        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_DAMAGE_MODIFIER_ID(), modifierType, damageModifier, EntityAttributeModifier.Operation.ADDITION));
        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_SPEED_MODIFIER_ID(), modifierType, speedModifier, EntityAttributeModifier.Operation.ADDITION));

        if (item instanceof MiningToolItem) {
            ((MiningToolAccessor) item).setAttackDamage(damageModifier);
            ((MiningToolAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof SwordItem) {
            ((SwordAccessor) item).setAttackDamage(damageModifier);
            ((SwordAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof TridentItem) {
            ((TridentAccessor) item).setAttributeModifiers(itemBuilder.build());
        }
    }

    public static int getTicksPerAttackOf(PlayerEntity player) {
        return (int)(20d / player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
    }

    public static double getAttackChargeProgress(PlayerEntity player) {
        int lastAttackTicks = ((LivingEntityAccessor)player).getLastAttackedTicks();
        double attackSpeed = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
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
        double toolReachBonus = Combat.getAttackRangeBonusOf(player.getEquippedStack(EquipmentSlot.MAINHAND));
        attackChargeProgress = Math.min(1.0d, attackChargeProgress);
        double chargeTimeBonus = attackChargeProgress * attackChargeProgress;
        double ridingBonus = player.hasVehicle() && player.getVehicle().isAlive() ? 0.5d : 0d;
        return Combat.PLAYER_BASE_ATTACK_REACH + chargeTimeBonus + toolReachBonus + ridingBonus;
    }

    public static double getAttackRange(PlayerEntity player) {
        return Combat.getAttackRange(player, Combat.getAttackChargeProgress(player));
    }

    public static void doSweepAttack(PlayerEntity player, double attackRange, int level) {
        if(level < 1) return;

        player.spawnSweepAttackParticles();
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(), 1.0F, 1.0F);

        if(player.getWorld() instanceof ServerWorld serverWorld) {
            attackRange = attackRange - (0.5d * (double)(4 - level));
            double attackRangeSquared = attackRange * attackRange;

            // Attack entities:
            Vec3d playerPos = player.getPos();
            Box boundingBox = new Box(playerPos.x - attackRange, playerPos.y - attackRange, playerPos.z - attackRange, playerPos.x + attackRange, playerPos.y + attackRange, playerPos.z + attackRange);
            List<LivingEntity> entitiesInRange = serverWorld.getEntitiesByClass(LivingEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
            for (LivingEntity targetEntity : entitiesInRange) {
                if(!player.isTeammate(targetEntity)) {
                    if (targetEntity.squaredDistanceTo(playerPos) < attackRangeSquared && Combat.isLookingTowards(player, targetEntity.getPos())) {
                        if(player.canSee(targetEntity)) {
                            player.setSprinting(true);
                            ((LivingEntityAccessor) player).setLastAttackedTicks(20);
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
                            blockState = serverWorld.getBlockState(blockPos);
                            if (blockState.getHardness(serverWorld, blockPos) == 0.0) {
                                serverWorld.breakBlock(blockPos, true, player);
                                above = blockPos.add(0, 1, 0);
                                if (serverWorld.getBlockState(above).getHardness(serverWorld, above) == 0.0F) {
                                    serverWorld.breakBlock(above, true, player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static double fastSquareRoot(double x){
        double d = 289358932.0;
        return Double.longBitsToDouble(((Double.doubleToLongBits( d )-(1l<<52) )>>1 ) + ( 1l<<61 ) );
    }

    public static boolean isLookingTowards(LivingEntity looker, Vec3d targetPos){
        return Combat.isLookingTowards(looker,targetPos,false);
    }

    public static boolean isLookingTowards(LivingEntity looker, Vec3d targetPos, boolean strict){
        return Combat.isLookingTowards(looker,targetPos,strict ? -0.75 : -0.5);
    }

    public static boolean isLookingTowards(LivingEntity looker, Vec3d targetPos, double dotProductThreshold){
        if(looker==null || targetPos == null) return false;
        Vec3d rotationVector = looker.getRotationVec(1.0F);
        Vec3d positionVector = targetPos.relativize(looker.getEyePos()).normalize();
        return (positionVector.dotProduct(rotationVector) < dotProductThreshold);
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

    public static Box getMobAttackBox(MobEntity mob) {
        Entity ridingEntity = mob.getVehicle();
        Box attackBox;
        if (ridingEntity != null) {
            Box box = ridingEntity.getBoundingBox();
            Box box2 = mob.getBoundingBox();
            attackBox = new Box(Math.min(box2.minX, box.minX), box2.minY, Math.min(box2.minZ, box.minZ), Math.max(box2.maxX, box.maxX), box2.maxY, Math.max(box2.maxZ, box.maxZ));
        } else {
            attackBox = mob.getBoundingBox();
            attackBox.offset(0d, mob.getEyeHeight(mob.getPose()), 0d);
        }
        double attackRangeBonus = Combat.getAttackRangeBonusOf(mob.getEquippedStack(EquipmentSlot.MAINHAND));
        return attackBox.expand(0.8 + attackRangeBonus, attackRangeBonus/2, 0.8 + attackRangeBonus);
    }

    public static Box getEntityHitbox(LivingEntity entity) {
        Box box = entity.getBoundingBox();
        Entity ridingEntity = entity.getVehicle();
        if (ridingEntity != null) {
            return box.withMinY(box.minY + ridingEntity.getMountedHeightOffset());
        } else {
            return box;
        }
    }
}