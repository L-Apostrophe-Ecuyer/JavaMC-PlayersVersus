package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;


public class WitheredZombieEntity extends ZombieEntity {
    public WitheredZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createWitheredAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.FOLLOW_RANGE, 7.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.33f)
                .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.ATTACK_KNOCKBACK, 1.2)
                .add(EntityAttributes.ARMOR, 8.0)
                .add(EntityAttributes.ARMOR_TOUGHNESS, 4.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(EntityAttributes.SPAWN_REINFORCEMENTS);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hasAttacked = super.tryAttack(target);
        if (hasAttacked && this.getMainHandStack().isEmpty() && target instanceof LivingEntity livingEntity) {
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60), this);
        }
        return hasAttacked;
    }

    @Override
    public void initCustomGoals() {
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, -1, 127));
        this.ambientSoundChance = -1000;
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.4F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 0.5F;
        this.goalSelector.add(2, new ZombieAttackGoal((ZombieEntity) ((Object)this), 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.7, 0.9F)); // Only 10% chance of actually wandering
        this.targetSelector.add(1, new RevengeGoal(this, PigEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, IronGolemEntity.class, false));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(source.isOf(DamageTypes.WITHER)) return false;
        return super.damage(source, amount);
    }

    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        int rand = random.nextInt(150);
        boolean isAtDiamondDepth = this.getBlockPos().getY() < -32;
        if (isAtDiamondDepth && rand % 23 == 0) {
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(5)));
            this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 1F;
        }

        if(isAtDiamondDepth && rand < 60) {
            this.armorDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.15f;
            if (rand < 30) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            else if (rand < 50) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            else if (rand < 60) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SHOVEL));
        }
        else if(rand < 20) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        else if(rand < 40) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        else if(rand < 60) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        else if(rand < 80) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        else if(rand < 90)this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
        if(rand < 90) {
            int damageAmount = (isAtDiamondDepth && rand < 60) ? rand + 900 : rand/2 + 150;
            this.getEquippedStack(EquipmentSlot.MAINHAND).setDamage(damageAmount);
            this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.15F;
        }

        // Bit less attack damage when wielding weapons:
        if(this.getEquippedStack(EquipmentSlot.MAINHAND).isDamageable()) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            entityAttributeInstance.setBaseValue(1.0);
        }
    }

    @Override
    public void setBaby(boolean baby) {
        return;
    }

    @Override
    protected boolean canConvertInWater() {
        return false;
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.ENTITY_HUSK_STEP;
    }

}
