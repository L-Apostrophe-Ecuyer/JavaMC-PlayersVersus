package frootloops.versus.mixin.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ImpalingEnchantment;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
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
    public float getAttackDamage(int level, EntityGroup group) {
        return (float)level * 0.5f;
    }
}