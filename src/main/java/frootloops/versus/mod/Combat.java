package frootloops.versus.mod;


import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.LivingEntityAccessor;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
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

    public static final double TRIDENT_SPEED = 1.0, TRIDENT_DAMAGE = 9.0, TRIDENT_REACH = 0.5;
    public static final double PICKAXE_SPEED = 1.2, PICKAXE_DAMAGE = 2.0, PICKAXE_REACH = 0.0;
    public static final double SHOVEL_SPEED = 1.4, SHOVEL_DAMAGE = 3.0, SHOVEL_REACH = 0.0;
    public static final double SWORD_SPEED = 1.6, SWORD_DAMAGE = 3.0, SWORD_REACH = 0.0;
    public static final double HOE_SPEED = 2.0, HOE_DAMAGE = 1.0, HOE_REACH = 0.5;
    public static final double AXE_SPEED = 1.0, AXE_DAMAGE = 6.0, AXE_REACH = 0.0;
    public static final Identifier ATTACK_REACH_MODIFIER_ID = Identifier.of(VersusMod.MOD_ID,"attack_reach_modifier");
    public static final Identifier ATTACK_KNOCKBACK_MODIFIER_ID = Identifier.of(VersusMod.MOD_ID,"attack_knockback_modifier");

    public static EntityAttribute CRITICAL_ATTACK_DAMAGE_ATTRIBUTE, SPRINT_ATTACK_DAMAGE_ATTRIBUTE;
    public static RegistryEntry<EntityAttribute> CRITICAL_ATTACK_DAMAGE, SPRINT_ATTACK_DAMAGE;

    static {
        // Register critical attack damage
        CRITICAL_ATTACK_DAMAGE_ATTRIBUTE = Registry.register(
                Registries.ATTRIBUTE,
                Identifier.of(VersusMod.MOD_ID, "critical_attack_damage"),
                new ClampedEntityAttribute("attribute.name.critical_attack_damage", 2.0, 0.0, 2048.0).setTracked(true)
        );

        // Register sprint attack damage
        SPRINT_ATTACK_DAMAGE_ATTRIBUTE = Registry.register(
                Registries.ATTRIBUTE,
                Identifier.of(VersusMod.MOD_ID, "sprint_attack_damage"),
                new ClampedEntityAttribute("attribute.name.sprint_attack_damage", 2.0, 0.0, 2048.0).setTracked(true)
        );
    }


    public static final float MIN_COOLDOWN_TO_SWING = 0.6f;

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 0.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 3.0d;
    public static final double PLAYER_MAX_ATTACK_SPEED = 2.5d;


    public static void onInitialize() {
        CRITICAL_ATTACK_DAMAGE = Registries.ATTRIBUTE.getEntry(CRITICAL_ATTACK_DAMAGE_ATTRIBUTE);
        SPRINT_ATTACK_DAMAGE = Registries.ATTRIBUTE.getEntry(SPRINT_ATTACK_DAMAGE_ATTRIBUTE);
    }

    private static double getCappedAttackSpeedOf(PlayerEntity player) {
        return Math.min(PLAYER_MAX_ATTACK_SPEED, player.getAttributeValue(EntityAttributes.ATTACK_SPEED));
    }

    public static int getTicksPerAttackOf(PlayerEntity player) {
        return (int)(20d / Combat.getCappedAttackSpeedOf(player));
    }

    public static double getAttackChargeProgress(PlayerEntity player) {
        return player.getAttackCooldownProgress(0.0f);
    }

    public static double getAttackRangeBonusOf(ItemStack itemStack) {
        if(itemStack == null || itemStack.isEmpty()) return 0.0d;
        if(itemStack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
            AttributeModifiersComponent attributeModifiersComponent = itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            for (AttributeModifiersComponent.Entry modifier : attributeModifiersComponent.modifiers()) {
                if(modifier.attribute() == EntityAttributes.ENTITY_INTERACTION_RANGE && modifier.slot() == AttributeModifierSlot.MAINHAND) {
                    return modifier.modifier().value();
                }
            }
        }
        return 0.0d;
    }

    public static double getAttackRange(PlayerEntity player, float attackChargeProgress) {
        double reachAttributeValue = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        attackChargeProgress = Math.min(1.0f, attackChargeProgress - 0.5f);
        double chargeTimeBonus = attackChargeProgress * attackChargeProgress;
        double ridingBonus = player.hasVehicle() && player.getVehicle().isAlive() ? 0.5d : 0d;
        return reachAttributeValue + chargeTimeBonus + ridingBonus;
    }

    public static double getAttackRange(PlayerEntity player) {
        return Combat.getAttackRange(player, player.getAttackCooldownProgress(0.0f));
    }

    public static boolean isInAttackRangeOf(PlayerEntity player, Entity entity, float attackCooldownProgress) {
        double range = Combat.getAttackRange(player, attackCooldownProgress);
        return (range * range) > player.getEyePos().squaredDistanceTo(entity.getEyePos());
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
        return canPlayerSprint(player.getHungerManager(), player.hasStatusEffect(StatusEffects.HUNGER));
    }

    public static boolean canPlayerSprint(HungerManager hungerManager, boolean hasHungerEffect) {
        if(!VersusSettings.Combat.DO_FOOD_OVERHAUL) return hungerManager.getFoodLevel() > 6;
        if(hasHungerEffect) return false;
        if(hungerManager.getFoodLevel() != 0) return true;
        if(hungerManager.getSaturationLevel() == 0.0f) return false;
        return true;
    }

    public enum AttackType {
        NORMAL, SPRINT, CRITICAL
    }

    public static AttackType getAttackType(LivingEntity entity) {
        if(entity instanceof PlayerEntity player) return getAttackType(player, getAttackChargeProgress(player));
        return AttackType.NORMAL;
    }

    public static AttackType getAttackType(PlayerEntity player, double attackCharge) {
        if(attackCharge < 0.9) return AttackType.NORMAL;
        if(player.isSprinting() && player.isOnGround()) return AttackType.SPRINT;
        if(player.fallDistance > 0.0
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasBlindnessEffect()
                && !player.hasVehicle()) return AttackType.CRITICAL;
        return AttackType.NORMAL;
    }
}