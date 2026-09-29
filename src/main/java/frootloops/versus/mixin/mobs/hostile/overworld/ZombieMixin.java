package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.MobSpawning;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster {
    protected ZombieMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    protected boolean convertsInWater() {return true;}

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Overwrite
    public void addBehaviourGoals() {
        Zombie self = ((Zombie) ((Object)this));
        this.goalSelector.addGoal(2, new ZombieAttackGoal(self, 1.0, false));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, true, 4, self::canBreakDoors));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(self).setAlertOthers(Pig.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(self, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(self, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(self, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(self, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        double followRange = 24.0;
        double mvtSpeed = 0.28;
        cir.setReturnValue(Monster.createMonsterAttributes()
                        .add(Attributes.FOLLOW_RANGE, followRange)
                        .add(Attributes.MOVEMENT_SPEED, mvtSpeed)
                        .add(Attributes.ATTACK_DAMAGE, 3.0)
                        .add(Attributes.ATTACK_KNOCKBACK, 1.1)
                        .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
    }

    @Inject(method = "hurtServer", at = @At("TAIL"), cancellable = true)
    private void damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.is(DamageTypes.FREEZE)) {
            this.convertTo(ModEntities.FROSTED_ZOMBIE, ConversionParams.single(this, true, true), zombie -> {
                if (!this.isSilent()) {
                    this.level().levelEvent(null, LevelEvent.SOUND_ZOMBIE_TO_DROWNED, this.blockPosition(), 0);
                }
            });
        }
        else if(source.is(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
            this.convertTo(ModEntities.WITHERED_ZOMBIE, ConversionParams.single(this, true, true), zombie -> {
                if (!this.isSilent()) {
                    this.level().levelEvent(null, LevelEvent.SOUND_ZOMBIE_TO_DROWNED, this.blockPosition(), 0);
                }
            });
        }
    }

    /*
    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return super.canSpawn(world, spawnReason);
        BlockPos pos = this.getBlockPos();
        if(pos.getY() < -16) return false;
        if(world.getLightLevel(LightType.SKY, pos) > 4) return false;
        if(!world.getBlockState(pos.down()).isIn(MobSpawning.UNDEAD_OVERWORLD_SPAWNABLE)) return false;
        return super.canSpawn(world, spawnReason);
    }*/

    @Override
    protected void pickUpItem(ServerLevel world, ItemEntity itemEntity) {
        if(itemEntity.getAge() > 160) super.pickUpItem(world, itemEntity);
    }


    @Override
    public void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        super.populateDefaultEquipmentSlots(random, localDifficulty);
        if(this.isBaby()) return;

        float difficulty = this.level().getDifficulty() == Difficulty.HARD ? 0.25f : 0.15f;
        ((Zombie)((Object)this)).setCanBreakDoors(true);
        if(!this.getClass().equals(Zombie.class)) return;
        if(random.nextBoolean()) return;

        float depth = Math.max(8.0f, 96.0f - (float)this.blockPosition().getY());
        float worldDepthExtraDifficulty = (depth * depth)/16384.0f;
        boolean haDifficultyBonusFromDepth = random.nextFloat() < (difficulty + worldDepthExtraDifficulty);

        int rand = random.nextInt(150);
        if (haDifficultyBonusFromDepth) {
            if(rand % 2 == 0 || rand % 7 == 0) {
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.getItemBySlot(EquipmentSlot.CHEST).setDamageValue(rand + 80);
            }
            if(rand % 3 == 0 || rand % 8 == 0) {
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.getItemBySlot(EquipmentSlot.LEGS).setDamageValue(rand + 80);
            }
            if((rand % 4 == 0 || rand % 9 == 0) && this.getY() < 56) {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.getItemBySlot(EquipmentSlot.HEAD).setDamageValue(rand + 80);
            }
            if(rand % 5 == 0) {
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                this.getItemBySlot(EquipmentSlot.FEET).setDamageValue(rand + 80);
            }
            if(rand % 13 == 0 && depth > 64) {
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.getItemBySlot(EquipmentSlot.CHEST).setDamageValue(rand + 384);
                this.setDropChance(EquipmentSlot.CHEST, 0.08F);
            }

            boolean isDeepInCave = this.convertsInWater() && this.blockPosition().getY() < 28;
            if(isDeepInCave) {
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0f);
                this.setHealth(24.0f);
            }

            if (isDeepInCave && rand % 23 == 0) {
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(5)));
                this.setDropChance(EquipmentSlot.OFFHAND, 1F);
            }

            if(isDeepInCave && rand < 60) {
                this.setDropChance(EquipmentSlot.MAINHAND, 0.15F);
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
                int damageAmount = (isDeepInCave && rand < 60) ? rand + 900 : rand/2 + 150;
                this.getItemBySlot(EquipmentSlot.MAINHAND).setDamageValue(damageAmount);
                this.setDropChance(EquipmentSlot.MAINHAND, 0.15F);
            }
        }
        else if(rand <= 55) {

            if(rand % 4 == 0) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.OAK_LOG,  random.nextInt(4) + rand/4));
            else if(rand % 5 == 0) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.OAK_PLANKS,  random.nextInt(12) + rand/2));
            else if(rand % 7 == 0) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BRICK,  random.nextInt(8) + rand));
            else if(rand % 11 == 0) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BRICK,  random.nextInt(6) + rand));
            else this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COPPER_INGOT,  random.nextInt(4)));
            this.setDropChance(EquipmentSlot.OFFHAND, 0.8F);
        }
        else {
            this.setHealth(16.0f);
        }

        // Bit less attack damage when wielding weapons:
        if(this.getItemBySlot(EquipmentSlot.MAINHAND).isDamageableItem()) {
            AttributeInstance entityAttributeInstance = this.getAttribute(Attributes.ATTACK_DAMAGE);
            entityAttributeInstance.setBaseValue(1.0);
        }
    }

    @Overwrite
    public static boolean getSpawnAsBabyOdds(RandomSource random) {
        return random.nextFloat() < 0.02f;
    }


    @Inject(method = "setBaby", at = @At(value = "TAIL"), cancellable = false)
    public void babiesArentNinjas(boolean baby, CallbackInfo info) {
        if (this.level() != null && !this.level().isClientSide()) {
            this.setHealth(12.0f);
        }
    }
}