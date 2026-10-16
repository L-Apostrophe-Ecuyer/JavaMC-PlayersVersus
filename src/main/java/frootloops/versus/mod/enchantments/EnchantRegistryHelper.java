package frootloops.versus.mod.enchantments;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;


public abstract class EnchantRegistryHelper {

    private static HolderGetter enchRegistryLookup = null;

    public static Holder<Enchantment> getRegistryEntry(Level world, ResourceKey<Enchantment> enchantment) {
        if(enchRegistryLookup == null) enchRegistryLookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        Optional<Holder.Reference<Enchantment>> enchantmentEntry = enchRegistryLookup.get(enchantment);
        if(enchantmentEntry.isPresent()) return enchantmentEntry.get();
        else {
            enchRegistryLookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return enchRegistryLookup.getOrThrow(enchantment);
        }
    }

    public static int getLevel(Level world, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if(!stack.isEnchanted()) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(getRegistryEntry(world, enchantment), stack);
    }

    public static int getLevel(ItemStack stack, Holder<Enchantment> enchantmentRegistryEntry) {
        ItemEnchantments itemEnchantmentsComponent = stack.get(DataComponents.ENCHANTMENTS);
        if(itemEnchantmentsComponent == null || itemEnchantmentsComponent.isEmpty()) return 0;
        return itemEnchantmentsComponent.getLevel(enchantmentRegistryEntry);
    }

    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if(!stack.isEnchanted()) return false;
        Iterator<Holder<Enchantment>> iterator = stack.getEnchantments().keySet().iterator();
        while (iterator.hasNext()) {
            Holder<Enchantment> enchant = iterator.next();
            if (enchant.unwrapKey().get() == enchantment) return true;
        }
        return false;
    }

    public static int getEquipmentLevel(Level world, LivingEntity user, ResourceKey<Enchantment> enchantment) {
        return EnchantmentHelper.getEnchantmentLevel(getRegistryEntry(world, enchantment), user);
    }

    public static EnchantmentInstance getMostImportantEnchant(List<EnchantmentInstance> list) {
        if(list == null || list.size() < 1) return null;
        if(list.size() == 1) return list.getFirst();
        EnchantmentInstance maxEnchant = null;
        int maxPower = Integer.MIN_VALUE;
        for(EnchantmentInstance e : list) {
            int power = getValueOfEnchantment(e);
            if(power > maxPower) {
                maxEnchant = e;
                maxPower = power;
            }
        }
        return maxEnchant;
    }
    public static int getValueOfEnchantment(EnchantmentInstance e) {
        Enchantment enchant = e.enchantment().value();
        int exclusiveEnchantBonus = enchant.exclusiveSet().size() * 8;
        int avgPower = (enchant.getMinCost(e.level()) + enchant.getMaxCost(e.level()))/2;
        int anvilCost = enchant.getAnvilCost();
        return avgPower + anvilCost + exclusiveEnchantBonus;
    }
}
