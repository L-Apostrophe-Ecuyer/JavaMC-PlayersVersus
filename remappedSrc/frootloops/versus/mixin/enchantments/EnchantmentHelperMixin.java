package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Overwrite
    public static List<EnchantmentLevelEntry> getPossibleEntries(int level, ItemStack stack, Stream<RegistryEntry<Enchantment>> possibleEnchantments) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
        boolean bl = stack.isOf(Items.BOOK);
        possibleEnchantments.filter(enchantment -> (((Enchantment)enchantment.value()).isPrimaryItem(stack) || bl)).forEach(enchantmentRegistryEntry -> {

            boolean canApplyEnchanment = true;
            int currentLevel = 0;
            if(!itemEnchantmentsComponent.isEmpty()) {
                for (RegistryEntry<Enchantment> itemEnchantment : itemEnchantmentsComponent.getEnchantments()) {
                    if(itemEnchantment.equals(enchantmentRegistryEntry)) {
                        break;
                    }else if(!Enchantment.canBeCombined(enchantmentRegistryEntry, itemEnchantment)) {
                        canApplyEnchanment = false;
                        break;
                    }
                }
                currentLevel = Enchants.getLevel(stack, enchantmentRegistryEntry);
            }
            if(canApplyEnchanment) {
                Enchantment enchantmentToAdd = (Enchantment) enchantmentRegistryEntry.value();
                for (int j = enchantmentToAdd.getMaxLevel(); j >= Math.max(currentLevel + 1, enchantmentToAdd.getMinLevel()); --j) {
                    int minPower = enchantmentToAdd.getMinPower(j - currentLevel) + currentLevel;
                    int maxPower = enchantmentToAdd.getMaxPower(j - currentLevel) + currentLevel;
                    if (level < minPower || level > maxPower) continue;
                    list.add(new EnchantmentLevelEntry((RegistryEntry<Enchantment>) enchantmentRegistryEntry, j));
                    break;
                }
            }
        });
        return list;
    }
}
