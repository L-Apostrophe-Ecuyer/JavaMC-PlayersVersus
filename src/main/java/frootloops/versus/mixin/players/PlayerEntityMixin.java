package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.block.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
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

    @Shadow public int totalExperience, experienceLevel, enchantingTableSeed;

    @Overwrite
    public double getEntityInteractionRange() {
        return Combat.getAttackRange((PlayerEntity) ((Object)this));
    }

    @Inject(method = "createPlayerAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
                DefaultAttributeContainer.builder()
                    .add(Registries.ATTRIBUTE.getEntry(Combat.CRITICAL_ATTACK_DAMAGE_ATTRIBUTE))
                    .add(Registries.ATTRIBUTE.getEntry(Combat.SPRINT_ATTACK_DAMAGE_ATTRIBUTE))
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
                    .add(EntityAttributes.MAX_HEALTH)
                    .add(EntityAttributes.KNOCKBACK_RESISTANCE)
                    .add(EntityAttributes.ARMOR)
                    .add(EntityAttributes.ARMOR_TOUGHNESS)
                    .add(EntityAttributes.MAX_ABSORPTION)
                    .add(EntityAttributes.STEP_HEIGHT)
                    .add(EntityAttributes.SCALE)
                    .add(EntityAttributes.GRAVITY)
                    .add(EntityAttributes.SAFE_FALL_DISTANCE)
                    .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER)
                    .add(EntityAttributes.JUMP_STRENGTH)
                    .add(EntityAttributes.OXYGEN_BONUS)
                    .add(EntityAttributes.BURNING_TIME)
                    .add(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                    .add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY)
                    .add(EntityAttributes.MOVEMENT_EFFICIENCY)
                    .add(EntityAttributes.ATTACK_KNOCKBACK)
        );
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if(amount > 0f && this.isSleeping()) {
            ((PlayerEntity)((Object)this)).wakeUp(false, true);
            if(source.getAttacker() instanceof LivingEntity attackingEntity) {
                this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, attackingEntity.getEyePos());
            }
        }
        if (VersusSettings.DO_FOOD_EATING_INTERRUPTION && source.getAttacker() != null && amount > 1.0F) {
            int maxUseTime = activeItemStack.getMaxUseTime(this);
            if (maxUseTime < 64) {
                itemUseTimeLeft = maxUseTime;
            }
        }
        return super.damage(world, source, amount);
    }

    @Inject(method = "canHarvest", at = @At("RETURN"), cancellable = true)
    public void canMineCopperWithWood(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && this.getMainHandStack().isOf(Items.WOODEN_PICKAXE) && (state.getSoundGroup() == BlockSoundGroup.COPPER || state.isOf(Blocks.COPPER_ORE) || state.isOf(Blocks.RAW_COPPER_BLOCK))) cir.setReturnValue(true);
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if(abilities.creativeMode) cir.setReturnValue(Float.MAX_VALUE);
        float breakingSpeed = cir.getReturnValue();
        if (!this.isOnGround()) breakingSpeed *= this.getVelocity().y >= 0.0 ? 4f : 3f;

        Block block = blockState.getBlock();
        if(block == Blocks.COBWEB) {
            cir.setReturnValue(breakingSpeed * 0.75f + 6f);
            return;
        }

        if(blockState.getSoundGroup() == BlockSoundGroup.DEEPSLATE) {
            ToolComponent toolComponent = this.getMainHandStack().getOrDefault(DataComponentTypes.TOOL, null);
            if(toolComponent == null) return;
            else if(!toolComponent.isCorrectForDrops(Blocks.DIAMOND_ORE.getDefaultState())) {
                cir.setReturnValue(breakingSpeed/3f);
            }
            else if(toolComponent.isCorrectForDrops(Blocks.OBSIDIAN.getDefaultState())) {
                VersusMod.MOD_LOGGER.warn("Mining speed: from " + breakingSpeed + " to " + (breakingSpeed + (toolComponent.defaultMiningSpeed() >= 9.0F ? 2.0f : 0.5f)));
                cir.setReturnValue(breakingSpeed + (toolComponent.defaultMiningSpeed() >= 9.0F ? 2.0f : 0.5f));
            }
            return;
        }

        if(blockState.isBurnable() && blockState.getSoundGroup() == BlockSoundGroup.WOOD) {
            cir.setReturnValue(breakingSpeed + 2f);
            return;
        }

        if(block instanceof ShulkerBoxBlock) {
            cir.setReturnValue(breakingSpeed + 8f);
            return;
        }

        if(block instanceof BrushableBlock) {
            cir.setReturnValue(breakingSpeed - 0.6f);
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

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 1)
    private float modifyAttackDamage(float amount) {
        boolean isSprinting = this.isSprinting() && this.isOnGround();
        boolean isCriticalHit = !isSprinting && !this.isOnGround() && !this.isInFluid();
        if(isSprinting) amount = Math.max(1.0f, amount + (float)this.getAttributeValue(Combat.SPRINT_ATTACK_DAMAGE));
        else if(isCriticalHit) amount = Math.max(1.0f, amount + (float)this.getAttributeValue(Combat.CRITICAL_ATTACK_DAMAGE));
        return amount;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void attackTypes(Entity target, CallbackInfo ci) {
        if(this.getWorld().isClient) return;

        // After attacking, the shield is interrupted:
        if(this.getOffHandStack().getItem() instanceof ShieldItem) {
            this.clearActiveItem();
            itemCooldownManager.set(this.getOffHandStack(), 6);
        }

        double amount = this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        boolean isSprinting = this.isSprinting();
        boolean isCriticalHit = !isSprinting && !this.isOnGround();
        if(isCriticalHit) return;

        if(amount < 1.0) {
            if (target instanceof LivingEntity livingEntity) {
                double strength = isSprinting ? 0.8 : 0.6;
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
                livingEntity.takeKnockback(strength, this.getX() - target.getX(), this.getZ() - target.getZ());
            }
            else if (target instanceof VehicleEntity || target instanceof ArmorStandEntity) {
                target.damage((ServerWorld) this.getWorld(), this.getDamageSources().playerAttack((PlayerEntity)((Object)this)), 2.0f);
            }
            ci.cancel(); // Cancel attack
        }
    }


    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 3)
    private boolean doSweepingAttacksOnRegularSwings(boolean isSweep) {
        return isSweep && (EnchantRegistryHelper.getLevel(this.getWorld(), this.getMainHandStack(), Enchantments.SWEEPING_EDGE) >= VersusSettings.MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS);
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
        if(sprinting && !Combat.canPlayerSprint(this.hungerManager, this.hasStatusEffect(StatusEffects.HUNGER))) return;
        super.setSprinting(sprinting);
    }
}