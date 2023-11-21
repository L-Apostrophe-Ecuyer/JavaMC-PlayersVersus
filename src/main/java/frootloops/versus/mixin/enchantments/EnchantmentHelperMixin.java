package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Overwrite
    public static ItemStack enchant(Random random, ItemStack stack, int level, boolean treasureAllowed) {
        List<EnchantmentLevelEntry> listCandidateEnchantments = EnchantmentHelper.generateEnchantments(random, stack, level, treasureAllowed);

        // Add new enchantments, or improve old ones:
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        for (EnchantmentLevelEntry entry : listCandidateEnchantments) {
            if(!enchantments.containsKey(entry.enchantment) || entry.level > enchantments.get(entry.enchantment)) {
                enchantments.put(entry.enchantment, entry.level);
            }
        }

        stack.removeSubNbt("Enchantments");
        stack.removeSubNbt("StoredEnchantments");
        EnchantmentHelper.set(enchantments, stack);
        if (stack.isOf(Items.BOOK)) stack = new ItemStack(Items.ENCHANTED_BOOK);
        return stack;
    }


    @Overwrite
    public static List<EnchantmentLevelEntry> getPossibleEntries(int power, ItemStack stack, boolean treasureAllowed) {
        ArrayList<EnchantmentLevelEntry> listPossibleEntries = Lists.newArrayList();
        Map<Enchantment, Integer> currentEnchantments = EnchantmentHelper.get(stack);
        boolean isBook = stack.isOf(Items.BOOK) || stack.isOf(Items.ENCHANTED_BOOK);

        forEachEnchant: for (Enchantment enchantment : Registries.ENCHANTMENT) {

            // Is the enchantment allowed on this item?
            boolean isEnchantmentIllegal = ((enchantment.isTreasure() && !treasureAllowed) || !enchantment.isAvailableForRandomSelection() || (!enchantment.isAcceptableItem(stack) && !isBook));
            if (isEnchantmentIllegal) continue forEachEnchant;

            // Does the enchantment play nice with other enchantments already on the item?
            for (Enchantment currentEnchant : currentEnchantments.keySet()) {
                if(currentEnchant != enchantment && !currentEnchant.canCombine(enchantment)) continue forEachEnchant;
            }

            // If the item already has the enchantment, give a cost for upgrades:
            int currentLevel = currentEnchantments.containsKey(enchantment) ? currentEnchantments.get(enchantment) : 0;
            int upgradeCost = currentLevel > 0 ? enchantment.getMinPower(currentLevel)/2 : 0;

            // Get the level of the new enchantment, and add it to the list:
            int minLevel = Math.max(currentLevel, enchantment.getMinLevel() - 1);
            for (int i = enchantment.getMaxLevel(); i > minLevel; --i) {
                if (power < enchantment.getMinPower(i) + upgradeCost || power > enchantment.getMaxPower(i) + upgradeCost/2) continue;
                if (i <= currentLevel) continue;
                listPossibleEntries.add(new EnchantmentLevelEntry(enchantment, i));
                continue forEachEnchant;
            }
        }
        return listPossibleEntries;
    }
}
