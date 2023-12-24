package frootloops.versus.mixin.players;

import com.google.common.collect.Multimap;
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
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.BlockSoundGroup;
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

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world, ItemCooldownManager itemCooldownManager) {
        super(entityType, world);
        this.itemCooldownManager = itemCooldownManager;
    }

    @Shadow private final ItemCooldownManager itemCooldownManager;
    @Shadow private ItemStack selectedItem;

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

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if (!this.isOnGround()) cir.setReturnValue(cir.getReturnValue() * 3f);
        if(this.getMainHandStack().getItem() instanceof ToolItem toolItem && toolItem.getMaterial() == ToolMaterials.WOOD) cir.setReturnValue(cir.getReturnValue() * 1.2f);

        if(blockState.isOf(Blocks.COBWEB)) {
            cir.setReturnValue(cir.getReturnValue() * 0.75f + 6f);
            return;
        }

        if(blockState.getBlock() instanceof ShulkerBoxBlock) {
            cir.setReturnValue(cir.getReturnValue() + 5f);
            return;
        }

        if(blockState.getSoundGroup() == BlockSoundGroup.DEEPSLATE) {
            if(this.getMainHandStack().getItem() instanceof PickaxeItem pickaxeItem) {
                if(pickaxeItem.getMaterial().getMiningLevel() < 2) {
                    cir.setReturnValue(cir.getReturnValue()/3f);
                }
                else if(pickaxeItem.getMaterial().getMiningLevel() >= 4) {
                    cir.setReturnValue(cir.getReturnValue() * 1.15f);
                }
            }
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


    @ModifyVariable(method = "damage", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, DamageSource source, float amount) {

        // Falling doesn't hurt as much:
        if (source.isIn(DamageTypeTags.IS_FALL))
            return amount/1.75f;

        // Explosions don't hurt as much, or at least, the damage is more consistent:
        if (source.isIn(DamageTypeTags.IS_EXPLOSION) && amount > 3.0f) {
            amount = (amount + amount/4.0f + 16.0f) / 4.0f;
            return  Math.min(amount, 30.0f);
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
        if (source.getAttacker() != null && amount > 1.0F) {
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
}