package frootloops.versus.mixin.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentPropertiesMixin {

    @Inject(method = "getMaxPower", at = @At("RETURN"), cancellable = true)
    public void getMaxPower(int level, CallbackInfoReturnable<Integer> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self instanceof ProtectionEnchantment) {
            cir.setReturnValue(60 + level * 10);
            cir.cancel();
        }
    }

    @Inject(method = "getMinPower", at = @At("RETURN"), cancellable = true)
    public void getMinPower(int level, CallbackInfoReturnable<Integer> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self instanceof ProtectionEnchantment) {
            cir.setReturnValue(cir.getReturnValue()/2 + (level - 1) * 20);
            cir.cancel();
        }
    }

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    public void getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self instanceof ProtectionEnchantment) {
            cir.setReturnValue(2);
            cir.cancel();
        }
    }

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    public void isAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self == Enchantments.SWEEPING_EDGE) {
            cir.setReturnValue(stack.getItem() instanceof ShovelItem || stack.getItem() instanceof SwordItem);
            cir.cancel();
        }
        else if (self == Enchantments.LOOTING) {
            cir.setReturnValue(stack.getItem() instanceof TridentItem || stack.getItem() instanceof SwordItem);
            cir.cancel();
        }
        else if (self == Enchantments.KNOCKBACK) {
            cir.setReturnValue(stack.getItem() instanceof ShovelItem || stack.getItem() instanceof MaceItem ||stack.getItem() instanceof TridentItem || stack.getItem() instanceof SwordItem);
            cir.cancel();
        }
    }

    @Inject(method = "isTreasure", at = @At("HEAD"), cancellable = true)
    public void isTreasure(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self == Enchantments.FIRE_ASPECT || self == Enchantments.DENSITY) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "isAvailableForEnchantedBookOffer", at = @At("HEAD"), cancellable = true)
    public void isAvailableForEnchantedBookOffer(CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) ((Object)this);
        if(self == Enchantments.SHARPNESS || self == Enchantments.POWER) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}