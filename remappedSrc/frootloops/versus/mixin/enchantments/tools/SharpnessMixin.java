package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DamageEnchantment.class)
public class SharpnessMixin extends Enchantment {
    protected SharpnessMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes, int typeIndex) {
        super(weight, type, slotTypes);
        this.typeIndex = typeIndex;
    }

    @Shadow
    public final int typeIndex;

    @Override
    public float getAttackDamage(int level, EntityGroup group) {
        if (this.typeIndex == 0) {
            return (float)level * 0.5f;
        }
        if (this.typeIndex == 1 && group == EntityGroup.UNDEAD) {
            return (float)level * 1f;
        }
        if (this.typeIndex == 2 && group == EntityGroup.ARTHROPOD) {
            return (float)level * 3.5f;
        }
        return 0.0f;
    }

    @Override
    public int getMinPower(int level) {
        int basePower = this.typeIndex == 0 ? 10 : 16;
        int powerPerLevel = this.typeIndex == 0 ? 12 : 6;
        return basePower + (level - 1) * powerPerLevel;
    }

    @Override
    public Rarity getRarity() {
        return this.typeIndex != 0 ? Rarity.RARE : Rarity.VERY_RARE;
    }


    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof TridentItem);
    }

}