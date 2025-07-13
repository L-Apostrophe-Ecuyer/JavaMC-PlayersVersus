package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(ZombieEntity.class)
public abstract class ZombieMixin extends HostileEntity {
    protected ZombieMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected boolean canConvertInWater() {return true;}

    @Override
    public int getMinAmbientSoundDelay() {
        return 500;
    }

    @Overwrite
    public void initCustomGoals() {
        ZombieEntity self = ((ZombieEntity) ((Object)this));
        this.goalSelector.add(2, new ZombieAttackGoal(self, 1.0, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, self::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.targetSelector.add(1, new RevengeGoal(self).setGroupRevenge(PigEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal(self, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal(self, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(self, IronGolemEntity.class, true));
        this.targetSelector.add(5, new ActiveTargetGoal(self, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Inject(method = "createZombieAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        double followRange = 24.0;
        double mvtSpeed = 0.28;
        cir.setReturnValue(HostileEntity.createHostileAttributes()
                        .add(EntityAttributes.FOLLOW_RANGE, followRange)
                        .add(EntityAttributes.MOVEMENT_SPEED, mvtSpeed)
                        .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
                        .add(EntityAttributes.ATTACK_KNOCKBACK, 1.1)
                        .add(EntityAttributes.SPAWN_REINFORCEMENTS));
    }

    @Inject(method = "damage", at = @At("TAIL"), cancellable = true)
    private void damage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.isOf(DamageTypes.FREEZE)) {
            this.convertTo(ModEntities.FROSTED_ZOMBIE, EntityConversionContext.create(this, true, true), zombie -> {
                if (!this.isSilent()) {
                    this.getWorld().syncWorldEvent(null, WorldEvents.ZOMBIE_CONVERTS_TO_DROWNED, this.getBlockPos(), 0);
                }
            });
        }
        else if(source.isOf(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
            this.convertTo(ModEntities.WITHERED_ZOMBIE, EntityConversionContext.create(this, true, true), zombie -> {
                if (!this.isSilent()) {
                    this.getWorld().syncWorldEvent(null, WorldEvents.ZOMBIE_CONVERTS_TO_DROWNED, this.getBlockPos(), 0);
                }
            });
        }
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return super.canSpawn(world, spawnReason);
        BlockPos pos = this.getBlockPos();
        if(pos.getY() < -16) return false;
        if(pos.getY() < 32 && !world.getBlockState(pos.down()).isOf(Blocks.STONE)) return false;
        if(world.getBlockState(pos.down()).isIn(BlockTags.AXE_MINEABLE)) return false;
        return super.canSpawn(world, spawnReason);
    }

    @Override
    protected void loot(ServerWorld world, ItemEntity itemEntity) {
        if(itemEntity.getItemAge() > 160) super.loot(world, itemEntity);
    }


    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(this.isBaby()) return;

        float difficulty = this.getWorld().getDifficulty() == Difficulty.HARD ? 0.25f : 0.15f;
        ((ZombieEntity)((Object)this)).setCanBreakDoors(true);
        if(!this.getClass().equals(ZombieEntity.class)) return;
        if(random.nextBoolean()) return;

        float depth = Math.max(8.0f, 96.0f - (float)this.getBlockPos().getY());
        float worldDepthExtraDifficulty = (depth * depth)/16384.0f;
        boolean haDifficultyBonusFromDepth = random.nextFloat() < (difficulty + worldDepthExtraDifficulty);

        int rand = random.nextInt(150);
        if (haDifficultyBonusFromDepth) {
            if(rand % 2 == 0 || rand % 7 == 0) {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.getEquippedStack(EquipmentSlot.CHEST).setDamage(rand + 80);
            }
            if(rand % 3 == 0 || rand % 8 == 0) {
                this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.getEquippedStack(EquipmentSlot.LEGS).setDamage(rand + 80);
            }
            if(rand % 4 == 0 || rand % 9 == 0) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.getEquippedStack(EquipmentSlot.HEAD).setDamage(rand + 80);
            }
            if(rand % 5 == 0) {
                this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getEquippedStack(EquipmentSlot.FEET).setDamage(rand + 80);
            }
            if(rand % 13 == 0 && depth > 64) {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.getEquippedStack(EquipmentSlot.CHEST).setDamage(rand + 384);
                this.armorDropChances[EquipmentSlot.CHEST.getEntitySlotId()] = 0.08f;
            }

            boolean isAtDiamondDepth = this.canConvertInWater() && this.getBlockPos().getY() < 8;
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
        }
        else if(rand <= 55) {

            if(rand % 4 == 0) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.OAK_LOG,  random.nextInt(4) + rand/4));
            else if(rand % 5 == 0) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.OAK_PLANKS,  random.nextInt(12) + rand/2));
            else if(rand % 7 == 0) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.BRICK,  random.nextInt(8) + rand));
            else if(rand % 11 == 0) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.BRICK,  random.nextInt(6) + rand));
            else this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.COPPER_INGOT,  random.nextInt(4)));

            this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 0.9F;
        }
        else {
            this.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(16.0f);
            this.setHealth(16.0f);
        }

        // Bit less attack damage when wielding weapons:
        if(this.getEquippedStack(EquipmentSlot.MAINHAND).isDamageable()) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            entityAttributeInstance.setBaseValue(1.0);
        }
    }

    @Overwrite
    public static boolean shouldBeBaby(Random random) {
        return random.nextFloat() < 0.02f;
    }


    @Inject(method = "setBaby", at = @At(value = "TAIL"), cancellable = false)
    public void babiesArentNinjas(boolean baby, CallbackInfo info) {
        if (this.getWorld() != null && !this.getWorld().isClient) {
            this.setHealth(12.0f);
        }
    }
}