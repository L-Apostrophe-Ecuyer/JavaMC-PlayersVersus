package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
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


    @Inject(method = "createZombieAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        double followRange = 48.0;
        double mvtSpeed = 0.28;
        cir.setReturnValue(HostileEntity.createHostileAttributes()
                        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, followRange)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, mvtSpeed)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                        .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.1)
                        .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                        .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS));
    }

    @Inject(method = "damage", at = @At("TAIL"), cancellable = true)
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.isOf(DamageTypes.FREEZE)) {
            this.convertTo(ModEntities.FROSTED_ZOMBIE, true);
        }
        else if(source.isOf(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
            this.convertTo(ModEntities.WITHERED_ZOMBIE, false);
        }
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


    @Inject(method = "setBaby", at = @At(value = "TAIL"), cancellable = false)
    public void babiesArentNinjas(boolean baby, CallbackInfo info) {
        if (this.getWorld() != null && !this.getWorld().isClient) {
            this.setHealth(12.0f);
        }
    }
}