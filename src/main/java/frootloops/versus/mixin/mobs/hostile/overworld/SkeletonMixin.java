package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.MobSpawning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Skeleton.class)
public abstract class SkeletonMixin extends AbstractSkeleton {
    protected SkeletonMixin(EntityType<? extends AbstractSkeleton> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected int getHardAttackInterval() {
        return 40;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack itemStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        ItemStack itemStack2 = this.getProjectile(itemStack);
        AbstractArrow persistentProjectileEntity = this.getArrow(itemStack2, pullProgress, itemStack);
        if (this.level() instanceof ServerLevel serverWorld) {
            double vx = (target.getX() - 2.0 * target.getDeltaMovement().x) - this.getX();
            double vy = target.getY(0.3333333333333333) - persistentProjectileEntity.getY();
            double vz = target.getZ() - 2.0 * target.getDeltaMovement().x - this.getZ();
            double horizontalDistance = Math.sqrt(vx * vx + vz * vz);
            float divergence = (float)(14 - serverWorld.getDifficulty().getId() * 4);

            Projectile.spawnProjectileUsingShoot(
                    persistentProjectileEntity, serverWorld, itemStack2, vx, vy + horizontalDistance * 0.2F, vz, 1.6F, divergence
            );
        }
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }


    /*
    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return super.canSpawn(world, spawnReason);
        BlockPos pos = this.getBlockPos();
        if(world.getLightLevel(LightType.SKY, pos) > 4) return false;
        if(!world.getBlockState(pos.down()).isIn(MobSpawning.UNDEAD_OVERWORLD_SPAWNABLE)) return false;
        return super.canSpawn(world, spawnReason);
    }*/

    @Override
    @Nullable
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        this.setDropChance(EquipmentSlot.MAINHAND, 0.3F);
        int rand = random.nextInt(100);
        if(rand > 52){
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
            this.setHealth(16);
        }
        else {
            if (rand < 12) {
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
                if (rand % 4 == 1) {
                    this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
            } else if (rand < 24) {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
                if (rand % 3 == 1) {
                    this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                    this.getItemBySlot(EquipmentSlot.OFFHAND).setDamageValue(rand / 2 + 100);
                }
            } else if (rand < 38) {
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                if (this.getY() < 32) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                else this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                if (rand % 4 == 1) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                if (rand % 5 == 1) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            } else if (rand < 44) {
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            } else {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
                if (rand % 3 == 1) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                if (rand % 4 == 1) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                if (rand % 5 == 1) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
            }
        }
    }
}
