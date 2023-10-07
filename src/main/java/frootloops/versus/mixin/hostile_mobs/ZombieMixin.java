package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieMixin extends HostileEntity {
    protected ZombieMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected boolean canConvertInWater() {return true;}

    @Inject(method = "createZombieAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
                HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 8.0)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36f)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                        .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                        .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.03));
    }

    @Overwrite
    public void initCustomGoals() {
        this.getNavigation().setCanSwim(true);

        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.6F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 0.6F;

        this.goalSelector.add(2, new ZombieAttackGoal((ZombieEntity) ((Object)this), 1.0, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, ((ZombieEntity) ((Object)this))::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.7, 0.66F));
        this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[]{ZombifiedPiglinEntity.class}));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, IronGolemEntity.class, false));
        this.targetSelector.add(5, new ActiveTargetGoal(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        float difficulty = this.world.getDifficulty() == Difficulty.HARD ? 0.3f : 0.1f;

        float distanceFromGroundLevel = 100.0f - (float)this.getBlockPos().getY();
        float worldDepthExtraDifficulty = (distanceFromGroundLevel * distanceFromGroundLevel)/40000.0f;
        boolean haDifficultyBonusFromDepth = random.nextFloat() < (difficulty + worldDepthExtraDifficulty);

        if (haDifficultyBonusFromDepth) {
            int rand = random.nextInt(100);
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
            if(rand % 13 == 0) {
                this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.getEquippedStack(EquipmentSlot.CHEST).setDamage(rand + 250);
                if(rand < 65) this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1));
            }

            boolean isAtDiamondDepth = this.canConvertInWater() && this.getBlockPos().getY() < 8;
            if (isAtDiamondDepth && rand < 30) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            else if (isAtDiamondDepth && rand < 50) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            else if (isAtDiamondDepth && rand < 60) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SHOVEL));
            else if(rand < 20) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            else if(rand < 40) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            else if(rand < 60) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            else if(rand < 80) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            else if(rand < 90)this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
            if(rand < 90) {
                int damageAmount = (isAtDiamondDepth && rand < 60) ? rand + 900 : rand/2 + 150;
                this.getEquippedStack(EquipmentSlot.MAINHAND).setDamage(damageAmount);
                this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.4F;
            }
        }
    }

    @Override
    public void setBaby(boolean baby) {
        // No.
    }
}