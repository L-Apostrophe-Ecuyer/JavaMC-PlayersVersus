package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.Combat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
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
                    .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Combat.PLAYER_BASE_ATTACK_DAMAGE)
                    .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1f)
                    .add(EntityAttributes.GENERIC_ATTACK_SPEED,  Combat.PLAYER_BASE_ATTACK_SPEED)
                    .add(EntityAttributes.GENERIC_LUCK)
                    .add(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, 5.0)
                    .add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,  Combat.PLAYER_BASE_ATTACK_REACH)
                    .add(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED)
                    .add(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED)
                    .add(EntityAttributes.PLAYER_SNEAKING_SPEED)
                    .add(EntityAttributes.PLAYER_MINING_EFFICIENCY)
                    .add(EntityAttributes.PLAYER_SWEEPING_DAMAGE_RATIO)
        );
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean switchHeldItemsWithoutResettingCooldown(ItemStack selectedItem, ItemStack itemStack) {
        if (!ItemStack.areEqual(selectedItem, itemStack)) {
            this.selectedItem = itemStack.copy();
        }
        return false; // Always return false, to avoid resetting cooldown
    }

    @Inject(method = "getXpToDrop", at = @At("RETURN"), cancellable = true)
    public void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(((64 + this.totalExperience) >> 3) + (this.totalExperience >> 1));
        }
    }

    @Inject(method = "canHarvest", at = @At("RETURN"), cancellable = true)
    public void canMineCopperWithWood(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && this.getMainHandStack().isOf(Items.WOODEN_PICKAXE) && (state.getSoundGroup() == BlockSoundGroup.COPPER || state.isOf(Blocks.COPPER_ORE) || state.isOf(Blocks.RAW_COPPER_BLOCK))) cir.setReturnValue(true);
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if(abilities.creativeMode) cir.setReturnValue(Float.MAX_VALUE);
        if (!this.isOnGround()) cir.setReturnValue(cir.getReturnValue() * 3f);

        if(blockState.isOf(Blocks.COBWEB)) {
            cir.setReturnValue(cir.getReturnValue() * 0.75f + 6f);
            return;
        }

        if(blockState.getBlock() instanceof ShulkerBoxBlock) {
            cir.setReturnValue(cir.getReturnValue() + 8f);
            return;
        }

        if(blockState.getBlock() instanceof BrushableBlock) {
            cir.setReturnValue(cir.getReturnValue() - 0.5f);
            return;
        }
    }

    @Override
    protected float modifyAppliedDamage(DamageSource source, float amount) {
        if(source.isOf(DamageTypes.SONIC_BOOM) && this.getWorld() instanceof ServerWorld serverWorld) {
            float protectionAmount = EnchantmentHelper.getProtectionAmount(serverWorld, this, source);
            if (protectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, protectionAmount);
        }
        return super.modifyAppliedDamage(source, amount);
    }


    @ModifyVariable(method = "damage", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, DamageSource source, float amount) {

        // Explosions don't hurt as much, or at least, the damage is more consistent:
        if (source.isIn(DamageTypeTags.IS_EXPLOSION) && amount > 4.0f) {
            return (amount + amount + 16.0f) / 4.0f;
        }

        // Hitting blocks while flying no longer neglects helmet protection:
        // Should be datadriven
        /*if(source.isOf(DamageTypes.FLY_INTO_WALL)) {
            ItemStack helmetStack = this.getEquippedStack(EquipmentSlot.HEAD);
            if(helmetStack != null && helmetStack.getItem() != null && helmetStack.getItem() instanceof ArmorItem helmetItem) {

                float armorAmount = helmetItem.getProtection();
                float toughnessAmount = helmetItem.getToughness();
                float enchantmentProtectionAmount = (float)Math.max(
                        Enchants.getLevel(getWorld(), helmetStack, Enchants.IMPACT_PROTECTION),
                        Enchants.getLevel(getWorld(), helmetStack, Enchantments.PROTECTION)
                );
                if (enchantmentProtectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, enchantmentProtectionAmount);
                return DamageUtil.getDamageLeft(this, amount, source, armorAmount, toughnessAmount);
            }
        }*/
        return amount;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void onDamageInterruptEating(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (VersusSettings.DO_FOOD_EATING_INTERRUPTION && source.getAttacker() != null && amount > 1.0F) {
            Item item = this.activeItemStack.getItem();
            if (item.getComponents().contains(DataComponentTypes.FOOD) || item instanceof PotionItem) {
                this.clearActiveItem();
                itemCooldownManager.set(item, 32);
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void attackTypes(Entity target, CallbackInfo ci) {

        // After attacking, the shield is interrupted:
        if(this.getOffHandStack().getItem() instanceof ShieldItem) {
            this.clearActiveItem();
            itemCooldownManager.set(this.getOffHandStack().getItem(), 6);
        }

        double amount = this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if(amount < 0.75f && target instanceof LivingEntity livingEntity) {
            double strength = this.isSprinting() ? 0.8 : 0.6;
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
            livingEntity.takeKnockback(strength, this.getX() - target.getX(), this.getZ() - target.getZ());
        }
    }

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 4)
    private boolean noSweepOnRegularAttacks(boolean sweepLevel) {
        return false;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    public void attackEnchantmentEffects(Entity target, CallbackInfo ci) {
        // Attacking while walking backwards deals less knockback:
        boolean isStillOrWalkingBackwards = (this.isOnGround() && !this.isSprinting()) && (this.getVelocity().x == 0d) && (this.getVelocity().z == 0d);
        if(isStillOrWalkingBackwards) target.setVelocity(target.getVelocity().multiply(0.6d, 0.8d, 0.6d));
    }

    @Inject(method = "setLastDeathPos", at = @At("TAIL"))
    public void setFoodLevelAfterDeath(Optional<GlobalPos> lastDeathPos, CallbackInfo ci) {
        if(VersusSettings.DO_FOOD_REDUCED_ON_SPAWN) this.hungerManager.setFoodLevel(6);
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