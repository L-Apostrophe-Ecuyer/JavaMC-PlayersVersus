package frootloops.versus.mixin.players;

import com.google.common.collect.Multimap;
import frootloops.versus.VersusSettings;
import frootloops.versus.backported.items.equipment.MaceItem;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.enchantments.tools.TossingEnchantment;
import frootloops.versus.mod.Combat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

import static frootloops.versus.backported.items.FutureItems.LAST_WIND_CHARGE_USE_TIME;

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



    @Inject(method = "createPlayerAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Combat.PLAYER_BASE_ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.10000000149011612)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, Combat.PLAYER_BASE_ATTACK_SPEED)
                .add(EntityAttributes.GENERIC_LUCK));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean switchHeldItemsWithoutResettingCooldown(ItemStack selectedItem, ItemStack itemStack) {
        if (!ItemStack.areEqual(selectedItem, itemStack)) {
            this.selectedItem = itemStack.copy();
        }
        return false;
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
            cir.setReturnValue(cir.getReturnValue() + 5f);
            return;
        }
    }

    @Override
    protected float modifyAppliedDamage(DamageSource source, float amount) {
        if(source.isOf(DamageTypes.SONIC_BOOM)) {
            int protectionAmount = EnchantmentHelper.getProtectionAmount(this.getArmorItems(), source);
            if (protectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, protectionAmount);
        }
        return super.modifyAppliedDamage(source, amount);
    }


    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    public void handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if(fallDistance > 3 && LAST_WIND_CHARGE_USE_TIME.containsKey(this.uuid)) {
            if (this.getWorld().getTime() - LAST_WIND_CHARGE_USE_TIME.get(this.uuid) < 60L) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @ModifyVariable(method = "damage", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, DamageSource source, float amount) {

        // Explosions don't hurt as much, or at least, the damage is more consistent:
        if (source.isIn(DamageTypeTags.IS_EXPLOSION) && amount > 4.0f) {
            return (amount + amount + 16.0f) / 4.0f;
        }

        // Hitting blocks while flying no longer neglects helmet protection:
        if(source.isOf(DamageTypes.FLY_INTO_WALL)) {
            ItemStack helmet = this.getEquippedStack(EquipmentSlot.HEAD);
            if(helmet != null) {
                Multimap<EntityAttribute, EntityAttributeModifier> helmetAttributeModifiers = helmet.getAttributeModifiers(EquipmentSlot.HEAD);

                float armorAmount = 0.0f;
                for (EntityAttributeModifier modifier:helmetAttributeModifiers.get(EntityAttributes.GENERIC_ARMOR))
                    armorAmount += modifier.getValue();

                float toughnessAmount = 0.0f;
                for (EntityAttributeModifier modifier:helmetAttributeModifiers.get(EntityAttributes.GENERIC_ARMOR_TOUGHNESS))
                    toughnessAmount += modifier.getValue();

                float protectionAmount = (float)Math.max(EnchantmentHelper.getLevel(Enchants.IMPACT_PROTECTION, helmet), EnchantmentHelper.getLevel(Enchantments.PROTECTION, helmet));
                if (protectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, protectionAmount);

                return DamageUtil.getDamageLeft(amount, armorAmount, toughnessAmount);
            }
        }
        return amount;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void onDamageInterruptEating(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (VersusSettings.DO_FOOD_EATING_INTERRUPTION && source.getAttacker() != null && amount > 1.0F) {
            Item item = this.activeItemStack.getItem();
            if (item.isFood() || item instanceof PotionItem) {
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
    }

    @Override
    public void onAttacking(Entity target) {
        // After attacking with a mace, fall distance is reset to cancel fall damage:
        if(this.getMainHandStack().getItem() instanceof MaceItem && !this.isOnGround()) {
            float velocity = (float)this.getVelocity().y;
            if((velocity < -0.4 || this.fallDistance > 1.5F) && !this.isFallFlying()) {

                float extraDamageVelocity = (velocity * -24.0f) + (velocity * velocity * 8.0f) + (velocity * velocity * velocity * -4.0f);
                float extraDamageFallDistance = (this.fallDistance * 3F);

                target.damage(this.getDamageSources().playerAttack((PlayerEntity)((Object) this)), Math.max(extraDamageVelocity, extraDamageFallDistance));
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_NETHERITE_BLOCK_PLACE, this.getSoundCategory(), 1.0f, 1.0f);
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_ANVIL_LAND, this.getSoundCategory(), 0.1f, 0.05f);
            }
            fallDistance = -3.0f;
        }
        super.onAttacking(target);
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

        // Toss attack and enchantment:
        if (!this.isSneaking() && this.isOnGround() && this.getMainHandStack().getItem() instanceof ShovelItem) {
            TossingEnchantment.performTossAttack(this, target, 0.2 + 0.1 * (double)EnchantmentHelper.getEquipmentLevel(Enchants.TOSSING, this));
        }
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
        if(sprinting && this.hungerManager.getFoodLevel() == 0 && this.age - this.getLastAttackedTime() < 48) return; // No sprinting when damaged and no food points
        else super.setSprinting(sprinting);
    }
}