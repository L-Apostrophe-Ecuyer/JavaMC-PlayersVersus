package frootloops.versus.mixin;

import com.google.common.collect.Maps;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    private final Map<StatusEffect, StatusEffectInstance> activeStatusEffects = Maps.newHashMap();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 2)
    private float fasterWaterMovement(float h) {
        return this.isSprinting() ? h : h + 0.4f;
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
    public void attackEnchantmentEffects(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            LivingEntity self = ((LivingEntity) (Object) this);
            ItemStack mainhandStack = self.getMainHandStack();
            if (mainhandStack.isEmpty()) return;

            // Shovel attack and Tossing Enchantment:
            if (!this.isSneaking() && this.isOnGround() && mainhandStack.getItem() instanceof ShovelItem) {
                int tossLevel = Enchants.getLevel(getWorld(), mainhandStack, Enchants.TOSSING);
                Enchants.performTossAttack(self, target, 0.2 + 0.1 * (double)tossLevel);
            }

            // Other enchantments: Frost Aspect, Impaling
            if (!mainhandStack.hasEnchantments()) return;
            int frostLevel = Enchants.getLevel(getWorld(), mainhandStack, Enchants.FROST_ASPECT);
            if (frostLevel > 0) Enchants.performFrostAttack(self, target, frostLevel);

            if (!mainhandStack.hasEnchantments()) return;
            int impaleLevel = Enchants.getLevel(getWorld(), mainhandStack, Enchantments.IMPALING);
            if (impaleLevel > 0) Enchants.performImpalingAttack(self, target, frostLevel);
        }
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void modifyInvincibilityFrames(DamageSource source, float amount, CallbackInfoReturnable cir) {
        if(source.getAttacker() instanceof LivingEntity attacker) {

            // Modify Invincibility Frames:
            if (timeUntilRegen > 10) {
                if (source.isOf(DamageTypes.ARROW)) timeUntilRegen = 0;
                else if (timeUntilRegen > 16 && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) timeUntilRegen = 16;
            }

            // Curse of Ender Enchantment:
            if(Enchants.getEquipmentLevel(getWorld(), ((LivingEntity)(Object)this), Enchants.CURSE_OF_ENDER) > 0) {
                Enchants.onCurseOfEnderUserDamaged(((LivingEntity)(Object)this), attacker);
            }
        }
    }
}
