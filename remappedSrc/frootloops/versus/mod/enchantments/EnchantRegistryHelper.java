package frootloops.versus.mod.enchantments;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;


public abstract class EnchantRegistryHelper {

    private static RegistryEntryLookup enchRegistryLookup = null;

    public static RegistryEntry<Enchantment> getRegistryEntry(World world, RegistryKey<Enchantment> enchantment) {
        if(enchRegistryLookup == null) enchRegistryLookup = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        Optional<RegistryEntry.Reference<Enchantment>> enchantmentEntry = enchRegistryLookup.getOptional(enchantment);
        if(enchantmentEntry.isPresent()) return enchantmentEntry.get();
        else {
            enchRegistryLookup = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            return enchRegistryLookup.getOrThrow(enchantment);
        }
    }

    public static int getLevel(World world, ItemStack stack, RegistryKey<Enchantment> enchantment) {
        if(!stack.hasEnchantments()) return 0;
        return EnchantmentHelper.getLevel(getRegistryEntry(world, enchantment), stack);
    }

    public static int getLevel(ItemStack stack, RegistryEntry<Enchantment> enchantmentRegistryEntry) {
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
        if(itemEnchantmentsComponent == null || itemEnchantmentsComponent.isEmpty()) return 0;
        return itemEnchantmentsComponent.getLevel(enchantmentRegistryEntry);
    }

    public static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        if(!stack.hasEnchantments()) return false;
        Iterator<RegistryEntry<Enchantment>> iterator = stack.getEnchantments().getEnchantments().iterator();
        while (iterator.hasNext()) {
            RegistryEntry<Enchantment> enchant = iterator.next();
            if (enchant.getKey().get() == enchantment) return true;
        }
        return false;
    }

    public static int getEquipmentLevel(World world, LivingEntity user, RegistryKey<Enchantment> enchantment) {
        return EnchantmentHelper.getEquipmentLevel(getRegistryEntry(world, enchantment), user);
    }

    public static EnchantmentLevelEntry getMostImportantEnchant(List<EnchantmentLevelEntry> list) {
        if(list == null || list.size() < 1) return null;
        if(list.size() == 1) return list.getFirst();
        EnchantmentLevelEntry maxEnchant = null;
        int maxPower = Integer.MIN_VALUE;
        for(EnchantmentLevelEntry e : list) {
            int power = getValueOfEnchantment(e);
            if(power > maxPower) {
                maxEnchant = e;
                maxPower = power;
            }
        }
        return maxEnchant;
    }
    public static int getValueOfEnchantment(EnchantmentLevelEntry e) {
        Enchantment enchant = e.enchantment().value();
        int exclusiveEnchantBonus = enchant.exclusiveSet().size() * 8;
        int avgPower = (enchant.getMinPower(e.level()) + enchant.getMaxPower(e.level()))/2;
        int anvilCost = enchant.getAnvilCost();
        return avgPower + anvilCost + exclusiveEnchantBonus;
    }
}
