package frootloops.versus.mixin;

import com.google.common.collect.Maps;
import frootloops.versus.mod.enchantments.CustomEnchants;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.clays.BrownMudBlock;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.brewing.effects.HauntingStatusEffect;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;

import static net.minecraft.fluid.FlowableFluid.FALLING;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    private final Map<StatusEffect, StatusEffectInstance> activeStatusEffects = Maps.newHashMap();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "travelInFluid", at = @At("STORE"), ordinal = 2)
    private float fasterWaterMovement(float h) {
        if(this.getWorld().getFluidState(this.getBlockPos()).get(FALLING, false)) {
            this.setVelocity(this.getVelocity().add(0.0, -0.03, 0.0));
            return h;
        }
        else return isPlayer() && isSwimming() ? h : h + 0.3f;
    }

    @Inject(method = "applyMovementInput", at = @At("RETURN"), cancellable = true)
    private void applyMovementInput(Vec3d movementInput, float slipperiness, CallbackInfoReturnable<Vec3d> cir) {
        if (this.getBlockStateAtPos().isOf(CustomBlocks.BROWN_MUD) && BrownMudBlock.canWalkOnWetMud(this)) {
            Vec3d vec3d = this.getVelocity();
            cir.setReturnValue(new Vec3d(vec3d.x, 0.2, vec3d.z));
        }
    }

    @ModifyConstant(method = "travelInFluid", constant = @Constant(floatValue = 0.02f))
    private float applyBuoyancyEffect(float thisMixinIsOnlyCalledWhenInWater) {
        if(((LivingEntity)((Object)this)).hasStatusEffect(CustomStatusEffects.BUOYANCY)) {
            double amplifier = 1.0 + ((LivingEntity)((Object)this)).getStatusEffect(CustomStatusEffects.BUOYANCY).getAmplifier();
            this.setVelocity(this.getVelocity().add(0.0, this.getVelocity().getY() * 0.025 + 0.05 * amplifier, 0.0));
        }
        return 0.02f;
    }

    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), ordinal = 0)
    private double takeMoreKnockback(double strength) {
        return strength * 1.2;
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        ItemStack mainHand = ((LivingEntity)((Object)this)).getMainHandStack();
        if(((LivingEntity)((Object)this)) instanceof PathAwareEntity && mainHand != null){
            if(mainHand.getItem() instanceof AxeItem) cir.setReturnValue(24);
            else if(mainHand.getItem() instanceof HoeItem) cir.setReturnValue(10);
            else cir.setReturnValue(16);
        }
    }

    @Inject(method = "tryAttack", at = @At("TAIL"))
    public void attackEnchantmentEffects(ServerWorld world, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            LivingEntity self = ((LivingEntity) (Object) this);
            ItemStack mainhandStack = self.getMainHandStack();
            if (mainhandStack.isEmpty()) return;

            // Shovel attack and Tossing Enchantment:
            if (!this.isSneaking() && this.isOnGround() && mainhandStack.getItem() instanceof ShovelItem) {
                int tossLevel = Enchants.getLevel(getWorld(), mainhandStack, CustomEnchants.TOSSING);
                CustomEnchants.performTossAttack(world, self, target, 0.2 + 0.1 * (double)tossLevel);
            }

            // Other enchantments: Frost Aspect, Impaling
            if (!mainhandStack.hasEnchantments()) return;
            int frostLevel = Enchants.getLevel(getWorld(), mainhandStack, CustomEnchants.FROST_ASPECT);
            if (frostLevel > 0) CustomEnchants.performFrostAttack(world, self, target, frostLevel);

            if (!mainhandStack.hasEnchantments()) return;
            int impaleLevel = Enchants.getLevel(getWorld(), mainhandStack, Enchantments.IMPALING);
            if (impaleLevel > 0) CustomEnchants.performImpalingAttack(world, self, target, frostLevel);
        }
    }


    @ModifyVariable(method = "damage", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, ServerWorld world, DamageSource source, float amount) {
        // Explosions don't hurt as much, or at least, the damage is more consistent:
        if (source.isIn(DamageTypeTags.IS_EXPLOSION) && amount > 4.0f) {
            return (amount + amount + 16.0f) / 4.0f;
        }
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            StatusEffectInstance fireResistanceEffect = ((LivingEntity)((Object)this)).getStatusEffect(StatusEffects.FIRE_RESISTANCE);
            if(fireResistanceEffect != null) return (fireResistanceEffect.getAmplifier() > 0) ? 0.0f : 0.6f;
        }
        return amount;
    }

    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"), index = 0)
    private RegistryEntry<StatusEffect> rebalancedFireResistance(RegistryEntry<StatusEffect> effect) {
        if(effect == StatusEffects.FIRE_RESISTANCE) {
            StatusEffectInstance fireResistanceEffect = ((LivingEntity)((Object)this)).getStatusEffect(StatusEffects.FIRE_RESISTANCE);
            if(fireResistanceEffect != null && fireResistanceEffect.getAmplifier() == 0) return StatusEffects.LUCK;
        }
        return effect;
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void modifyInvincibilityFrames(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(source.getAttacker() instanceof LivingEntity attacker) {

            // Modify Invincibility Frames:
            if (timeUntilRegen > 10) {
                if (source.isOf(DamageTypes.ARROW)) {
                    if(attacker.getMainHandStack().isOf(Items.CROSSBOW)) timeUntilRegen = 9;
                    else timeUntilRegen = 14;
                }
                else if (timeUntilRegen > 18 && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) timeUntilRegen = 18;
            }

            // Curse of Ender Enchantment:
            if(Enchants.getEquipmentLevel(getWorld(), ((LivingEntity)(Object)this), CustomEnchants.CURSE_OF_ENDER) > 0) {
                CustomEnchants.onCurseOfEnderUserDamaged(world, ((LivingEntity)(Object)this), attacker);
            }
        }
        else if (timeUntilRegen > 10 && source.isOf(DamageTypes.ARROW)) timeUntilRegen = 12;
    }

    @Inject(method = "onStatusEffectsRemoved", at = @At("HEAD"))
    private void onStatusEffectsRemoved(Collection<StatusEffectInstance> effects, CallbackInfo info) {
        if(this.getWorld().isClient) return;
        for(StatusEffectInstance statusEffectInstance : effects) {
            if (statusEffectInstance.getEffectType() == CustomStatusEffects.HAUNTING) {
                HauntingStatusEffect.removeEffect(((LivingEntity) (Object) this));
                return;
            }
        }
    }
}
