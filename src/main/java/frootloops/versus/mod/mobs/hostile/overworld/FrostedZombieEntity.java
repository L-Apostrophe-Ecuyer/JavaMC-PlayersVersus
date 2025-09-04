package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

public class FrostedZombieEntity extends ZombieEntity implements RangedAttackMob {
    public FrostedZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        //this.setFrozenTicks(-72000);
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        SnowballEntity snowballEntity = new SnowballEntity(this.getWorld(), this, this.getOffHandStack());
        double d = target.getEyeY() - (double)1.1f;
        double e = target.getX() - this.getX();
        double f = d - snowballEntity.getY();
        double g = target.getZ() - this.getZ();
        double h = Math.sqrt(e * e + g * g) * (double)0.2f;
        snowballEntity.setVelocity(e, f + h, g, 1.6f, 12.0f);
        this.playSound(SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 0.4f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        this.getWorld().spawnEntity(snowballEntity);
        this.swingHand(Hand.OFF_HAND);
        this.getOffHandStack().setCount(this.getOffHandStack().getCount() - 1);
    }

    static class SnowballAttackGoal extends ProjectileAttackGoal {
        public final FrostedZombieEntity frostedZombie;

        public SnowballAttackGoal(FrostedZombieEntity frostedZombie, double mobSpeed, int intervalTicks, float maxShootRange) {
            super(frostedZombie, mobSpeed, intervalTicks, maxShootRange);
            this.frostedZombie = frostedZombie;
        }
        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.frostedZombie.getOffHandStack().isOf(Items.SNOWBALL);
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if(!source.isIn(DamageTypeTags.IS_FREEZING)) return super.damage(world, source, amount);
        else return false;
    }

    public static boolean canMobSpawn(EntityType<? extends FrostedZombieEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        BlockPos blockPos = pos;
        while (world.getBlockState(blockPos = blockPos.up()).isOf(Blocks.POWDER_SNOW)) {}
        return HostileEntity.canSpawnInDark(type, world, spawnReason, pos, random) && (spawnReason == SpawnReason.SPAWNER || world.isSkyVisible(blockPos.down()));
    }

    public static DefaultAttributeContainer.Builder createFrostedAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.FOLLOW_RANGE, 14.0).add(EntityAttributes.MOVEMENT_SPEED, 0.25f).add(EntityAttributes.ATTACK_DAMAGE, 2.0).add(EntityAttributes.ARMOR, 2.0).add(EntityAttributes.SPAWN_REINFORCEMENTS);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean hasAttacked = super.tryAttack(world, target);
        if (hasAttacked && this.getMainHandStack().isEmpty() && target instanceof LivingEntity livingEntity) {
            livingEntity.setFrozenTicks(livingEntity.getFrozenTicks() + 80);
        }
        return hasAttacked;
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        int rand = random.nextInt(32);
        if(rand - 8 > 0) {
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SNOWBALL, rand - 8));
            this.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.8F);
            this.goalSelector.add(1, new SnowballAttackGoal(this, 1.25, 12, 12.0f));
        }
        if (rand < 4) {
            int i = random.nextInt(3);
            if (i == 0) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            else this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(2, new ZombieAttackGoal(this, 1.1, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.targetSelector.add(1, new ActiveTargetGoal<PlayerEntity>((MobEntity)this, PlayerEntity.class, true));
        this.targetSelector.add(2, new RevengeGoal(this, new Class[0]).setGroupRevenge(ZombifiedPiglinEntity.class));
        this.targetSelector.add(3, new ActiveTargetGoal<MerchantEntity>((MobEntity)this, MerchantEntity.class, false));
    }
}
