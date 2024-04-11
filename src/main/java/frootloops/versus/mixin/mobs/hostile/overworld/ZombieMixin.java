package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.VersusSettings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(ZombieEntity.class)
public abstract class ZombieMixin extends HostileEntity {
    protected ZombieMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    private static final UUID BABY_SPEED_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");

    @Shadow
    protected boolean canConvertInWater() {return true;}

    @Override
    public int getMinAmbientSoundDelay() {
        return 500;
    }

    @Shadow
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(ZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final UUID BABY_HEALTH_ID = UUID.fromString("AA766B59-9566-4402-BC1F-2EE2A276D836");

    // Reduced:
    private static final EntityAttributeModifier BABY_SPEED_BONUS_REDUCED = new EntityAttributeModifier(BABY_SPEED_ID, "Baby speed boost", 0.2, EntityAttributeModifier.Operation.MULTIPLY_BASE);
    private static final EntityAttributeModifier BABY_HEALTH_REDUCED = new EntityAttributeModifier(BABY_HEALTH_ID, "Baby health nerf", -12, EntityAttributeModifier.Operation.ADDITION);

    @Inject(method = "createZombieAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        double followRange = VersusSettings.DO_ZOMBIE_SOUND_DETECTION ? 7.0 : 30.0;
        double mvtSpeed = VersusSettings.DO_ZOMBIE_SOUND_DETECTION ? 0.32 : 0.3;
        cir.setReturnValue(HostileEntity.createHostileAttributes()
                        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, followRange)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, mvtSpeed)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                        .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                        .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.06));
    }

    @Overwrite
    public void initCustomGoals() {
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.4F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 0.5F;
        if(VersusSettings.DO_ZOMBIE_SOUND_DETECTION) this.ambientSoundChance = -1000;

        if(this.getY() > 56d || this.getSteppingBlockState().getSoundGroup() == BlockSoundGroup.GRASS) {
            this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, ((ZombieEntity) ((Object)this))::canBreakDoors));
        }

        this.goalSelector.add(2, new ZombieAttackGoal((ZombieEntity) ((Object)this), 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.7, 0.8F));
        this.targetSelector.add(1, (new RevengeGoal(this, PigEntity.class)));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, IronGolemEntity.class, false));
        this.targetSelector.add(5, new ActiveTargetGoal(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Override
    protected void loot(ItemEntity itemEntity) {
        if(itemEntity.getItemAge() > 160) super.loot(itemEntity);
    }


    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        if(this.isBaby()) return;

        float difficulty = this.getWorld().getDifficulty() == Difficulty.HARD ? 0.25f : 0.15f;
        ((ZombieEntity)((Object)this)).setCanBreakDoors(true);
        this.setCanPickUpLoot(true);

        float depth = Math.max(16.0f, 96.0f - (float)this.getBlockPos().getY());
        float worldDepthExtraDifficulty = (depth * depth)/32768.0f;
        boolean haDifficultyBonusFromDepth = random.nextFloat() < (difficulty + worldDepthExtraDifficulty);

        if (haDifficultyBonusFromDepth) {
            int rand = random.nextInt(150);
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
                ItemStack enchantedBook = EnchantmentHelper.enchant(this.getWorld().random, new ItemStack(Items.BOOK), 16, true);
;               this.equipStack(EquipmentSlot.OFFHAND, enchantedBook);
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

        // Bit less attack damage when wielding weapons:
        if(this.getEquippedStack(EquipmentSlot.MAINHAND).isDamageable()) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            entityAttributeInstance.setBaseValue(1.0);
        }
    }

    @Overwrite
    public static boolean shouldBeBaby(Random random) {
        return random.nextFloat() < 0.02f;
    }

    @Override
    public void setBaby(boolean baby) {
        this.getDataTracker().set(BABY, baby);
        if (this.getWorld() != null && !this.getWorld().isClient) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            entityAttributeInstance.removeModifier(BABY_SPEED_ID);
            if (baby) entityAttributeInstance.addTemporaryModifier(BABY_SPEED_BONUS_REDUCED);

            entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (baby) entityAttributeInstance.addTemporaryModifier(BABY_HEALTH_REDUCED);
            if (baby) this.setHealth(12.0f);
        }
    }
}