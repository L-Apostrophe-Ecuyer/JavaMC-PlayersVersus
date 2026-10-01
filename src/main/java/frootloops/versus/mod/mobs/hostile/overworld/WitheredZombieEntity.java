package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;



public class WitheredZombieEntity extends Zombie {
    public WitheredZombieEntity(EntityType<? extends Zombie> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createWitheredAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 6.5)
                .add(Attributes.MOVEMENT_SPEED, 0.33f)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.1)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean hasAttacked = super.doHurtTarget(world, target);
        if (hasAttacked && this.getMainHandItem().isEmpty() && target instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), this);
        }
        return hasAttacked;
    }

    @Override
    public void addBehaviourGoals() {
        this.addEffect(new MobEffectInstance(MobEffects.WITHER, -1, 127));
        this.ambientSoundTime = -1000;
        this.setDropChance(EquipmentSlot.MAINHAND, 0.4F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.5F);
        this.goalSelector.addGoal(2, new ZombieAttackGoal((Zombie) ((Object)this), 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.9F)); // Only 10% chance of actually wandering
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Pig.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, IronGolem.class, false));
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if(source.is(DamageTypes.WITHER)) return false;
        if(super.hurtServer(world, source, amount)) {
            this.playSound(SoundEvents.PLAYER_BREATH, 0.3f, (this.random.nextFloat() - this.random.nextFloat()) * 0.4F + 0.6F);
            return true;
        }
        else return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if(this.tickCount % 16 == 0 && this.getTarget() != null) {
            double distanceSquared = this.getTarget().distanceToSqr(this);
            if(this.getTarget().isShiftKeyDown()) distanceSquared *= 3;
            if(distanceSquared > 256.0) this.setTarget(null);
        }
    }

    @Override
    public void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        int rand = random.nextInt(150);
        boolean isAtDiamondDepth = this.blockPosition().getY() < -32;
        if (isAtDiamondDepth && rand % 23 == 0) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(5)));
            this.setDropChance(EquipmentSlot.OFFHAND, 1.0F);
        }

        if(isAtDiamondDepth && rand < 60) {
            this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
            if (rand < 30) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            else if (rand < 50) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            else if (rand < 60) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SHOVEL));
        }
        else if(rand < 20) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        else if(rand < 40) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        else if(rand < 60) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        else if(rand < 80) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        else if(rand < 90)this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
        if(rand < 90) {
            int damageAmount = (isAtDiamondDepth && rand < 60) ? rand + 900 : rand/2 + 150;
            this.getItemBySlot(EquipmentSlot.MAINHAND).setDamageValue(damageAmount);
        }

        // Bit less attack damage when wielding weapons:
        if(this.getItemBySlot(EquipmentSlot.MAINHAND).isDamageableItem()) {
            AttributeInstance entityAttributeInstance = this.getAttribute(Attributes.ATTACK_DAMAGE);
            entityAttributeInstance.setBaseValue(1.0);
        }
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        return this.getWalkTargetValue(this.blockPosition(), world) >= 0.0F;
    }

    @Override
    public void setBaby(boolean baby) {
        return;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }
}
