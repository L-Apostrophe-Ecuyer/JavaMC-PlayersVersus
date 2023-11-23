package frootloops.versus.mixin.enchantments.armor;

import com.google.common.collect.Maps;
import net.minecraft.enchantment.DepthStriderEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ThornsEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

@Mixin(DepthStriderEnchantment.class)
public class DepthStriderMixin extends Enchantment {
    protected DepthStriderMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
         return (stack.getItem() instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.LEGS);
    }

    @Override
    public Map<EquipmentSlot, ItemStack> getEquipment(LivingEntity entity) {
        EnumMap<EquipmentSlot, ItemStack> map = Maps.newEnumMap(EquipmentSlot.class);
        for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS}) {
            ItemStack itemStack = entity.getEquippedStack(equipmentSlot);
            if (itemStack.isEmpty()) continue;
            map.put(equipmentSlot, itemStack);
        }
        return map;
    }
}