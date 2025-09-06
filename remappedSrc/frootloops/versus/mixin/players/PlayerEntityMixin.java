package frootloops.versus.mixin.players;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world, ItemCooldownManager itemCooldownManager) {
        super(entityType, world);
        this.itemCooldownManager = itemCooldownManager;
    }

    @Shadow private final ItemCooldownManager itemCooldownManager;
    @Shadow private HungerManager hungerManager;
    @Shadow private ItemStack selectedItem;

    @Shadow private final PlayerAbilities abilities = new PlayerAbilities();

    @Shadow public int totalExperience;

    @Overwrite
    public double getEntityInteractionRange() {
        return Combat.getAttackRange((PlayerEntity) ((Object)this));
    }

    @Inject(method = "createPlayerAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
            LivingEntity.createLivingAttributes()
                    .add(EntityAttributes.ATTACK_DAMAGE, Combat.PLAYER_BASE_ATTACK_DAMAGE)
                    .add(EntityAttributes.MOVEMENT_SPEED, 0.1f)
                    .add(EntityAttributes.ATTACK_SPEED,  Combat.PLAYER_BASE_ATTACK_SPEED)
                    .add(EntityAttributes.LUCK)
                    .add(EntityAttributes.BLOCK_INTERACTION_RANGE, 5.0)
                    .add(EntityAttributes.ENTITY_INTERACTION_RANGE,  Combat.PLAYER_BASE_ATTACK_REACH)
                    .add(EntityAttributes.BLOCK_BREAK_SPEED)
                    .add(EntityAttributes.SUBMERGED_MINING_SPEED)
                    .add(EntityAttributes.SNEAKING_SPEED)
                    .add(EntityAttributes.MINING_EFFICIENCY)
                    .add(EntityAttributes.SWEEPING_DAMAGE_RATIO)
        );
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean switchHeldItemsWithoutResettingCooldown(ItemStack selectedItem, ItemStack itemStack) {
        if (!ItemStack.areEqual(selectedItem, itemStack)) {
            this.selectedItem = itemStack.copy();
        }
        return false; // Always return false, to avoid resetting cooldown
    }

    @Inject(method = "getXpToDrop", at = @At("HEAD"), cancellable = true)
    public void getXpToDrop(ServerWorld world, CallbackInfoReturnable<Integer> cir) {
        PlayerEntity player = (PlayerEntity)((Object)this);
        if(this.totalExperience == 0 || this.isExperienceDroppingDisabled() || world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || this.isSpectator()) {
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(((64 + this.totalExperience) >> 3) + (this.totalExperience >> 1) - 8);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = false)
    public void jumpInVehicles(Vec3d movementInput, CallbackInfo info) {
        if(this.jumping && this.hasVehicle() && this.getVehicle() instanceof BoatEntity boatEntity && !boatEntity.isSubmergedInWater()) {
            if(boatEntity.isOnGround() || (boatEntity.fallDistance == 0f && boatEntity.getY() == boatEntity.lastY)) {
                double jumpStrength = boatEntity.isOnGround() ? 0.225 : 0.425;
                Vec3d velocity = boatEntity.getVelocity();
                boatEntity.setVelocity(velocity.x, Math.max(jumpStrength, velocity.y), velocity.z);
                boatEntity.velocityDirty = true;
            }
        }
    }


    @Inject(method = "canHarvest", at = @At("RETURN"), cancellable = true)
    public void canMineCopperWithWood(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && this.getMainHandStack().isOf(Items.WOODEN_PICKAXE) && (state.getSoundGroup() == BlockSoundGroup.COPPER || state.isOf(Blocks.COPPER_ORE) || state.isOf(Blocks.RAW_COPPER_BLOCK))) cir.setReturnValue(true);
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if(abilities.creativeMode) cir.setReturnValue(Float.MAX_VALUE);
        float breakingSpeed = cir.getReturnValue();
        if (!this.isOnGround()) breakingSpeed *= 3f;

        Block block = blockState.getBlock();
        if(block == Blocks.COBWEB) {
            cir.setReturnValue(breakingSpeed * 0.75f + 6f);
            return;
        }

        if(blockState.isBurnable() && blockState.getSoundGroup() == BlockSoundGroup.WOOD) {
            cir.setReturnValue(breakingSpeed + 2f);
            return;
        }

        if(block.getHardness() == 6.0F && block.getDefaultMapColor() == MapColor.OFF_WHITE) {
            cir.setReturnValue(breakingSpeed * 2.4f);
            return;
        }

        if(block instanceof ShulkerBoxBlock) {
            cir.setReturnValue(breakingSpeed + 8f);
            return;
        }

        if(block instanceof BrushableBlock) {
            cir.setReturnValue(breakingSpeed - 0.5f);
            return;
        }
        cir.setReturnValue(breakingSpeed);
    }

    @Override
    protected float modifyAppliedDamage(DamageSource source, float amount) {
        if(source.isOf(DamageTypes.SONIC_BOOM) && this.getWorld() instanceof ServerWorld serverWorld) {
            float protectionAmount = EnchantmentHelper.getProtectionAmount(serverWorld, this, source);
            if (protectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, protectionAmount);
        }
        return super.modifyAppliedDamage(source, amount);
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void onDamageInterruptEating(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (VersusSettings.DO_FOOD_EATING_INTERRUPTION && source.getAttacker() != null && amount > 1.0F) {
            Item item = this.activeItemStack.getItem();
            if (item.getComponents().contains(DataComponentTypes.FOOD) || item instanceof PotionItem) {
                this.clearActiveItem();
                itemCooldownManager.set(this.activeItemStack, 32);
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void attackTypes(Entity target, CallbackInfo ci) {
        if(this.getWorld().isClient) return;

        // After attacking, the shield is interrupted:
        if(this.getOffHandStack().getItem() instanceof ShieldItem) {
            this.clearActiveItem();
            itemCooldownManager.set(this.getOffHandStack(), 6);
        }

        double amount = this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        if(amount < (this.isOnGround() ? 0.5f : 0.75f)) {
            if (target instanceof LivingEntity livingEntity) {
                double strength = this.isSprinting() ? 0.8 : 0.5;
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
                livingEntity.takeKnockback(strength, this.getX() - target.getX(), this.getZ() - target.getZ());
            }
            else if (target instanceof VehicleEntity || target instanceof ArmorStandEntity) {
                target.damage((ServerWorld) this.getWorld(), this.getDamageSources().playerAttack((PlayerEntity)((Object)this)), 2.0f);
            }
        }
    }


    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 3)
    private boolean doSweepingAttacksOnRegularSwings(boolean isSweep) {
        return isSweep && (Enchants.getLevel(this.getWorld(), this.getMainHandStack(), Enchantments.SWEEPING_EDGE) >= VersusSettings.MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    public void attackKnockbackKitingNerf(Entity target, CallbackInfo ci) {
        // Attacking while walking backwards deals less knockback:
        boolean isStillOrWalkingBackwards = (this.isOnGround() && !this.isSprinting()) && (this.getVelocity().x == 0d) && (this.getVelocity().z == 0d);

        // Attacking while walking backwards deals less knockback:
        if(isStillOrWalkingBackwards) target.setVelocity(target.getVelocity().multiply(0.4d, 0.8d, 0.4d));
        else if(target.getVelocity().lengthSquared() < 1.0 && target instanceof LivingEntity livingEntity) livingEntity.takeKnockback(0.4, this.getX() - target.getX(), this.getZ() - target.getZ());
    }

    @Override
    public void setUuid(UUID uuid) {
        if(VersusSettings.DO_FOOD_REDUCED_ON_SPAWN) this.hungerManager.setFoodLevel(6);
        super.setUuid(uuid);
    }

    @Inject(method = "canConsume", at = @At("HEAD"), cancellable = true)
    public void canConsume(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if(VersusSettings.DO_FOOD_OVERHAUL) {
            cir.setReturnValue(ignoreHunger || (this.hungerManager.isNotFull() && (this.hungerManager.getFoodLevel() < 6 + this.getMaxHealth() - this.getHealth())));
        }
    }

    @Override
    public void setSprinting(boolean sprinting) {
        //if(sprinting && this.hungerManager.getFoodLevel() == 0 && this.getHealth() < 20.0f && (this.age - this.lastDamageTime > 160)) return;  // No sprinting when damaged and no food points
        if(sprinting && !Combat.canPlayerSprint(this.hungerManager)) return;
        super.setSprinting(sprinting);
    }
}