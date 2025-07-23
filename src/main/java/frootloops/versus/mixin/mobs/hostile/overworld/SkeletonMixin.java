package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.MobSpawning;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SkeletonEntity.class)
public abstract class SkeletonMixin extends AbstractSkeletonEntity {
    protected SkeletonMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected int getHardAttackInterval() {
        return 40;
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        ItemStack itemStack = this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));
        ItemStack itemStack2 = this.getProjectileType(itemStack);
        PersistentProjectileEntity persistentProjectileEntity = this.createArrowProjectile(itemStack2, pullProgress, itemStack);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            double vx = (target.getX() - 2.0 * target.getVelocity().x) - this.getX();
            double vy = target.getBodyY(0.3333333333333333) - persistentProjectileEntity.getY();
            double vz = target.getZ() - 2.0 * target.getVelocity().x - this.getZ();
            double horizontalDistance = Math.sqrt(vx * vx + vz * vz);
            float divergence = (float)(14 - serverWorld.getDifficulty().getId() * 4);

            ProjectileEntity.spawnWithVelocity(
                    persistentProjectileEntity, serverWorld, itemStack2, vx, vy + horizontalDistance * 0.2F, vz, 1.6F, divergence
            );
        }
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
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
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.3F;

        int rand = random.nextInt(100);
        if(rand > 52){
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.1F;
            this.setHealth(16);
        }
        else {
            if (rand < 12) {
                this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
                if (rand % 4 == 1) {
                    this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
            } else if (rand < 24) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
                if (rand % 3 == 1) {
                    this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                    this.getEquippedStack(EquipmentSlot.OFFHAND).setDamage(rand / 2 + 100);
                }
            } else if (rand < 38) {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                if (this.getY() < 32) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                else this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                if (rand % 4 == 1) this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                if (rand % 5 == 1) this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
            } else if (rand < 44) {
                this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
                this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
            } else {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
                if (rand % 3 == 1) this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                if (rand % 4 == 1) this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                if (rand % 5 == 1) this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
            }
        }
    }
}
