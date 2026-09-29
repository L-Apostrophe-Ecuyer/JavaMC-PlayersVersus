package frootloops.versus.mixin;

import com.google.common.collect.Maps;
import frootloops.versus.mod.enchantments.CustomEnchants;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.clays.CustomMudBlock;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.brewing.effects.HauntingStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    private final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();

    @Shadow public final boolean addEffect(MobEffectInstance effect) {return false;}

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyVariable(method = "travelInFluid", at = @At("STORE"), ordinal = 2)
    private float fasterWaterMovement(float h) {
        return isAlwaysTicking() && isSwimming() ? h : h + 0.3f;
    }

    @Inject(method = "handleRelativeFrictionAndCalculateMovement", at = @At("RETURN"), cancellable = true)
    private void applyMovementInput(Vec3 movementInput, float slipperiness, CallbackInfoReturnable<Vec3> cir) {
        if (this.getInBlockState().is(CustomBlocks.BROWN_MUD) && CustomMudBlock.canWalkOnWetMud(this)) {
            Vec3 vec3d = this.getDeltaMovement();
            cir.setReturnValue(new Vec3(vec3d.x, 0.2, vec3d.z));
        }
    }

    @ModifyConstant(method = "travelInFluid", constant = @Constant(floatValue = 0.02f))
    private float applyBuoyancyEffect(float thisMixinIsOnlyCalledWhenInWater) {
        if(((LivingEntity)((Object)this)).hasEffect(CustomStatusEffects.BUOYANCY)) {
            double amplifier = this.isVisuallySwimming() ? 1.5 : this.isShiftKeyDown() ? 0.8 : 1.0 + ((LivingEntity)((Object)this)).getEffect(CustomStatusEffects.BUOYANCY).getAmplifier();
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.getDeltaMovement().y() * 0.03 + 0.05 * amplifier, 0.0));
        }
        return 0.02f;
    }

    @ModifyVariable(method = "knockback", at = @At("HEAD"), ordinal = 0)
    private double takeMoreKnockback(double strength) {
        return strength * 1.2;
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        ItemStack mainHand = ((LivingEntity)((Object)this)).getMainHandItem();
        if(((LivingEntity)((Object)this)) instanceof PathfinderMob && mainHand != null){
            if(mainHand.getItem() instanceof AxeItem) cir.setReturnValue(24);
            else if(mainHand.getItem() instanceof HoeItem) cir.setReturnValue(10);
            else cir.setReturnValue(16);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("TAIL"))
    public void attackEnchantmentEffects(ServerLevel world, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            LivingEntity self = ((LivingEntity) (Object) this);
            ItemStack mainhandStack = self.getMainHandItem();
            if (mainhandStack.isEmpty()) return;

            // Shovel attack and Tossing Enchantment:
            if (!this.isShiftKeyDown() && this.onGround() && mainhandStack.getItem() instanceof ShovelItem) {
                int tossLevel = EnchantRegistryHelper.getLevel(world, mainhandStack, CustomEnchants.TOSSING);
                CustomEnchants.performTossAttack(world, self, target, 0.2 + 0.1 * (double)tossLevel);
            }

            // Other enchantments: Frost Aspect, Impaling
            if (!mainhandStack.isEnchanted()) return;
            int frostLevel = EnchantRegistryHelper.getLevel(world, mainhandStack, CustomEnchants.FROST_ASPECT);
            if (frostLevel > 0) CustomEnchants.performFrostAttack(world, self, target, frostLevel);

            if (!mainhandStack.isEnchanted()) return;
            int impaleLevel = EnchantRegistryHelper.getLevel(world, mainhandStack, Enchantments.IMPALING);
            if (impaleLevel > 0) CustomEnchants.performImpalingAttack(world, self, target, frostLevel);
        }
    }


    @ModifyVariable(method = "hurtServer", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, ServerLevel world, DamageSource source, float amount) {

        // Fire resistance is only partial at level one!
        if (source.is(DamageTypeTags.IS_FIRE)) {
            MobEffectInstance fireResistanceEffect = ((LivingEntity)((Object)this)).getEffect(MobEffects.FIRE_RESISTANCE);
            if(fireResistanceEffect != null) return (fireResistanceEffect.getAmplifier() > 0) ? 0.0f : 0.6f;
        }

        // Random Drowning & Suffocation should no longer slowly kill pets:
        else if(amount > 0f && !this.isAlwaysTicking() && (source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) && (((LivingEntity)((Object)this)) instanceof AgeableMob)) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, true, false));
            return amount;
        }

        // Explosions don't hurt as much, or at least, the damage is more consistent:
        else if (source.is(DamageTypeTags.IS_EXPLOSION) && amount > 4.0f) {
            return (amount + amount + 16.0f) / 4.0f;
        }
        return amount;
    }

    @ModifyArg(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"), index = 0)
    private Holder<MobEffect> rebalancedFireResistance(Holder<MobEffect> effect) {
        if(effect == MobEffects.FIRE_RESISTANCE) {
            MobEffectInstance fireResistanceEffect = ((LivingEntity)((Object)this)).getEffect(MobEffects.FIRE_RESISTANCE);
            if(fireResistanceEffect != null && fireResistanceEffect.getAmplifier() == 0) return MobEffects.LUCK;
        }
        return effect;
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void modifyInvincibilityFrames(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.getEntity() instanceof Monster || source.getEntity() instanceof Player) {

            // Modify Invincibility Frames:
            if (invulnerableTime > 10) {
                if (source.is(DamageTypes.ARROW)) {
                    // Crossbow arrows don't trigger invincibility frames, allowing for multishot shotguns:
                    if(((LivingEntity)source.getEntity()).getMainHandItem().is(Items.CROSSBOW)) invulnerableTime = 9;
                    // Regular bow shots give only 4 ticks of invincibility
                    else invulnerableTime = 14;
                }
                // Anything else gives 8 ticks of invincibility
                else if (invulnerableTime > 18 && !source.is(DamageTypeTags.BYPASSES_ARMOR)) invulnerableTime = 18;
            }

            // Curse of Ender Enchantment:
            if(EnchantRegistryHelper.getEquipmentLevel(this.level(), ((LivingEntity)(Object)this), CustomEnchants.CURSE_OF_ENDER) > 0) {
                CustomEnchants.onCurseOfEnderUserDamaged(world, ((LivingEntity)(Object)this), source.getEntity());
            }
        }
        else if (invulnerableTime > 10 && source.is(DamageTypes.ARROW)) invulnerableTime = 12;
    }

    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void onStatusEffectsRemoved(Collection<MobEffectInstance> effects, CallbackInfo info) {
        if(this.level().isClientSide()) return;
        for(MobEffectInstance statusEffectInstance : effects) {
            if (statusEffectInstance.getEffect() == CustomStatusEffects.HAUNTING) {
                HauntingStatusEffect.removeEffect(((LivingEntity) (Object) this));
                return;
            }
        }
    }
}
