package frootloops.versus.mod;


import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class Combat {

    public static final double TRIDENT_SPEED = 1.0, TRIDENT_DAMAGE = 9.0, TRIDENT_REACH = 0.5;
    public static final double PICKAXE_SPEED = 1.2, PICKAXE_DAMAGE = 2.0, PICKAXE_REACH = 0.0;
    public static final double SHOVEL_SPEED = 1.4, SHOVEL_DAMAGE = 3.0, SHOVEL_REACH = 0.0;
    public static final double SWORD_SPEED = 1.6, SWORD_DAMAGE = 3.0, SWORD_REACH = 0.0;
    public static final double HOE_SPEED = 2.0, HOE_DAMAGE = 1.0, HOE_REACH = 0.5;
    public static final double AXE_SPEED = 1.0, AXE_DAMAGE = 6.0, AXE_REACH = 0.0;
    public static final ResourceLocation ATTACK_REACH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID,"attack_reach_modifier");
    public static final ResourceLocation ATTACK_KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID,"attack_knockback_modifier");

    public static Attribute CRITICAL_ATTACK_DAMAGE_ATTRIBUTE, SPRINT_ATTACK_DAMAGE_ATTRIBUTE;
    public static Holder<Attribute> CRITICAL_ATTACK_DAMAGE, SPRINT_ATTACK_DAMAGE;

    static {
        // Register critical attack damage
        CRITICAL_ATTACK_DAMAGE_ATTRIBUTE = Registry.register(
                BuiltInRegistries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "critical_attack_damage"),
                new RangedAttribute("attribute.name.critical_attack_damage", 2.0, 0.0, 2048.0).setSyncable(true)
        );

        // Register sprint attack damage
        SPRINT_ATTACK_DAMAGE_ATTRIBUTE = Registry.register(
                BuiltInRegistries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "sprint_attack_damage"),
                new RangedAttribute("attribute.name.sprint_attack_damage", 2.0, 0.0, 2048.0).setSyncable(true)
        );
    }


    public static final float MIN_COOLDOWN_TO_SWING = 0.6f;

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 0.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 3.0d;
    public static final double PLAYER_MAX_ATTACK_SPEED = 2.5d;


    public static void onInitialize() {
        CRITICAL_ATTACK_DAMAGE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CRITICAL_ATTACK_DAMAGE_ATTRIBUTE);
        SPRINT_ATTACK_DAMAGE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(SPRINT_ATTACK_DAMAGE_ATTRIBUTE);
    }

    private static double getCappedAttackSpeedOf(Player player) {
        return Math.min(PLAYER_MAX_ATTACK_SPEED, player.getAttributeValue(Attributes.ATTACK_SPEED));
    }

    public static int getTicksPerAttackOf(Player player) {
        return (int)(20d / Combat.getCappedAttackSpeedOf(player));
    }

    public static double getAttackChargeProgress(Player player) {
        return player.getAttackStrengthScale(0.0f);
    }

    public static double getAttackRangeBonusOf(ItemStack itemStack) {
        if(itemStack == null || itemStack.isEmpty()) return 0.0d;
        if(itemStack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
            ItemAttributeModifiers attributeModifiersComponent = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            for (ItemAttributeModifiers.Entry modifier : attributeModifiersComponent.modifiers()) {
                if(modifier.attribute() == Attributes.ENTITY_INTERACTION_RANGE && modifier.slot() == EquipmentSlotGroup.MAINHAND) {
                    return modifier.modifier().amount();
                }
            }
        }
        return 0.0d;
    }

    public static double getAttackRange(Player player, float attackChargeProgress) {
        double reachAttributeValue = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double chargeTimeMult = Math.min(1.0f, attackChargeProgress);
        double ridingBonus = player.isPassenger() && player.getVehicle().isAlive() ? 0.5d : 0d;
        return reachAttributeValue * chargeTimeMult + ridingBonus;
    }

    public static double getAttackRange(Player player) {
        return Combat.getAttackRange(player, player.getAttackStrengthScale(0.0f));
    }

    public static boolean isInAttackRangeOf(Player player, Entity entity, float attackCooldownProgress) {
        double range = Combat.getAttackRange(player, attackCooldownProgress);
        return (range * range) > player.getEyePosition().distanceToSqr(entity.getEyePosition());
    }

    public static final boolean isLookingTowardsEntity(LivingEntity looker, LivingEntity target, boolean strict){
        return Combat.isLookingTowards(looker,new Vec3(target.getX(), target.getEyeY(), target.getZ()),strict);
    }

    public static final boolean isLookingTowardsEntity(LivingEntity looker, LivingEntity target, double dotProductThreshold){
        return Combat.isLookingTowards(looker,new Vec3(target.getX(), target.getEyeY(), target.getZ()),dotProductThreshold);
    }


    public static final boolean isLookingTowards(LivingEntity looker, Vec3 targetPos){
        return Combat.isLookingTowards(looker,targetPos,false);
    }

    public static final boolean isLookingTowards(LivingEntity looker, Vec3 targetPos, boolean strict){
        return Combat.isLookingTowards(looker,targetPos,strict ? -0.75 : -0.5);
    }

    public static final boolean isLookingTowards(LivingEntity looker, Vec3 targetPos, double dotProductThreshold){
        if(looker==null || targetPos == null) return false;
        Vec3 rotationVector = looker.getViewVector(1.0F);
        Vec3 positionVector = targetPos.vectorTo(looker.getEyePosition());
        if(rotationVector.dot(positionVector) >= 0.0F) return false;
        else return (rotationVector.dot(positionVector.normalize()) < dotProductThreshold);
    }

    public static AABB getMobAttackBox(Mob mob, boolean jump) {
        Entity ridingEntity = mob.getVehicle();
        AABB attackBox;
        if (ridingEntity != null) {
            AABB box = ridingEntity.getBoundingBox();
            AABB box2 = mob.getBoundingBox();
            attackBox = new AABB(Math.min(box2.minX, box.minX), box2.minY, Math.min(box2.minZ, box.minZ), Math.max(box2.maxX, box.maxX), box2.maxY, Math.max(box2.maxZ, box.maxZ));
        }
        else if (mob instanceof EnderMan) {
            attackBox = mob.getBoundingBox().inflate(0.5d, mob.getEyeHeight(mob.getPose())/2 + 1d, 0.5d);
        }
        else if (jump || !mob.onGround()) {
            attackBox = mob.getBoundingBox().inflate(0d, mob.getEyeHeight(mob.getPose())/2 + 0.5d, 0d);
        }
        else {
            attackBox = mob.getBoundingBox().move(0d, mob.getEyeHeight(mob.getPose())/2, 0d);
        }
        double attackRangeBonus = Combat.getAttackRangeBonusOf(mob.getItemBySlot(EquipmentSlot.MAINHAND));
        return attackBox.inflate(0.8 + attackRangeBonus, attackRangeBonus/2, 0.8 + attackRangeBonus);
    }

    public static AABB getEntityHitbox(Entity entity) {
        AABB box = entity.getBoundingBox();
        Entity ridingEntity = entity.getVehicle();
        if (ridingEntity != null) {
            Vec3 vec3d = ridingEntity.getPassengerRidingPosition(entity);
            return box.setMinY(Math.max(vec3d.y, box.minY));
        } else {
            return box;
        }
    }

    public static boolean canPlayerSprint(Player player) {
        return canPlayerSprint(player.getFoodData(), player.hasEffect(MobEffects.HUNGER));
    }

    public static boolean canPlayerSprint(FoodData hungerManager, boolean hasHungerEffect) {
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
        if(entity instanceof Player player) return getAttackType(player, getAttackChargeProgress(player));
        return AttackType.NORMAL;
    }

    public static AttackType getAttackType(Player player, double attackCharge) {
        if(attackCharge < 0.9) return AttackType.NORMAL;
        if(player.isSprinting() && player.onGround()) return AttackType.SPRINT;
        if(player.fallDistance > 0.0
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isMobilityRestricted()
                && !player.isPassenger()) return AttackType.CRITICAL;
        return AttackType.NORMAL;
    }
}