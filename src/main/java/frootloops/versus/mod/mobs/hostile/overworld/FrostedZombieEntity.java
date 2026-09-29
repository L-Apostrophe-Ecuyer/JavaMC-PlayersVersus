package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class FrostedZombieEntity extends Zombie implements RangedAttackMob {
    public FrostedZombieEntity(EntityType<? extends Zombie> entityType, Level world) {
        super(entityType, world);
        //this.setFrozenTicks(-72000);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        Snowball snowballEntity = new Snowball(this.level(), this, this.getOffhandItem());
        double d = target.getEyeY() - (double)1.1f;
        double e = target.getX() - this.getX();
        double f = d - snowballEntity.getY();
        double g = target.getZ() - this.getZ();
        double h = Math.sqrt(e * e + g * g) * (double)0.2f;
        snowballEntity.shoot(e, f + h, g, 1.6f, 12.0f);
        this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0f, 0.4f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        this.level().addFreshEntity(snowballEntity);
        this.swing(InteractionHand.OFF_HAND);
        this.getOffhandItem().setCount(this.getOffhandItem().getCount() - 1);
    }

    static class SnowballAttackGoal extends RangedAttackGoal {
        public final FrostedZombieEntity frostedZombie;

        public SnowballAttackGoal(FrostedZombieEntity frostedZombie, double mobSpeed, int intervalTicks, float maxShootRange) {
            super(frostedZombie, mobSpeed, intervalTicks, maxShootRange);
            this.frostedZombie = frostedZombie;
        }
        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.frostedZombie.getOffhandItem().is(Items.SNOWBALL);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if(!source.is(DamageTypeTags.IS_FREEZING)) return super.hurtServer(world, source, amount);
        else return false;
    }

    public static boolean canMobSpawn(EntityType<? extends FrostedZombieEntity> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        BlockPos blockPos = pos;
        while (world.getBlockState(blockPos = blockPos.above()).is(Blocks.POWDER_SNOW)) {}
        return Monster.checkMonsterSpawnRules(type, world, spawnReason, pos, random) && (spawnReason == EntitySpawnReason.SPAWNER || world.canSeeSky(blockPos.below()));
    }

    public static AttributeSupplier.Builder createFrostedAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 14.0).add(Attributes.MOVEMENT_SPEED, 0.25f).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.ARMOR, 2.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean hasAttacked = super.doHurtTarget(world, target);
        if (hasAttacked && this.getMainHandItem().isEmpty() && target instanceof LivingEntity livingEntity) {
            livingEntity.setTicksFrozen(livingEntity.getTicksFrozen() + 80);
        }
        return hasAttacked;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        int rand = random.nextInt(32);
        if(rand - 8 > 0) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SNOWBALL, rand - 8));
            this.setDropChance(EquipmentSlot.OFFHAND, 0.8F);
            this.goalSelector.addGoal(1, new SnowballAttackGoal(this, 1.25, 12, 12.0f));
        }
        if (rand < 4) {
            int i = random.nextInt(3);
            if (i == 0) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            else this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(ZombifiedPiglin.class));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<AbstractVillager>((Mob)this, AbstractVillager.class, false));
    }
}
