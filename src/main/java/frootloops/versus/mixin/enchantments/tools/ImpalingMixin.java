package frootloops.versus.mixin.enchantments.tools;

import frootloops.versus.VersusMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ImpalingEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ImpalingEnchantment.class)
public class ImpalingMixin extends Enchantment {
    protected ImpalingMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if(target.isTouchingWaterOrRain()) {
            float extraDamageToWetMobs = level; // Note: for some reason, this is called twice. So I've reduced the damage.
            target.damage(user.getDamageSources().trident(user,user), extraDamageToWetMobs);
            user.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.1f, 1.0f);
        }
    }
}