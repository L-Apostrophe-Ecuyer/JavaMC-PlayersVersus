package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import net.fabricmc.fabric.mixin.datagen.loot.BlockLootTableGeneratorMixin;
import net.minecraft.data.server.loottable.BlockLootTableGenerator;
import net.minecraft.enchantment.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Overwrite
    public static ItemStack enchant(Random random, ItemStack stack, int level, boolean treasureAllowed) {

        // Does the enchantment play nice with other enchantments already on the item?
        int numProtectionEnchantments = 0;
        Map<Enchantment, Integer> currentEnchantments = EnchantmentHelper.get(stack);
        for (Enchantment currentEnchant : currentEnchantments.keySet()) {
            if(currentEnchant instanceof ProtectionEnchantment) numProtectionEnchantments += 1;
        }

        // Add new enchantments, or improve old ones:
        List<EnchantmentLevelEntry> listCandidateEnchantments = EnchantmentHelper.generateEnchantments(random, stack, level, treasureAllowed);
        for (EnchantmentLevelEntry candidate : listCandidateEnchantments) {
            if(!currentEnchantments.containsKey(candidate.enchantment) || candidate.level > currentEnchantments.get(candidate.enchantment)) {
                if(candidate.enchantment instanceof ProtectionEnchantment) {
                    if(numProtectionEnchantments >= 2) {
                        listCandidateEnchantments.remove(candidate);
                        continue;
                    }
                    else numProtectionEnchantments += 1;
                }
                currentEnchantments.put(candidate.enchantment, candidate.level);
            }
        }

        stack.removeSubNbt("Enchantments");
        stack.removeSubNbt("StoredEnchantments");
        if (stack.isOf(Items.BOOK)) stack = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.set(currentEnchantments, stack);
        return stack;
    }

    @Overwrite
    public static List<EnchantmentLevelEntry> generateEnchantments(Random random, ItemStack stack, int power, boolean treasureAllowed) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i <= 0) return list;

        // Does the enchantment play nice with other enchantments already on the item?
        int numProtectionEnchantments = 0;
        Map<Enchantment, Integer> currentEnchantments = EnchantmentHelper.get(stack);
        for (Enchantment currentEnchant : currentEnchantments.keySet()) {
            if(currentEnchant instanceof ProtectionEnchantment) numProtectionEnchantments += 1;
        }

        List<EnchantmentLevelEntry> listCandidateEnchantments = EnchantmentHelper.getPossibleEntries(power = MathHelper.clamp(Math.round((float)power), 1, Integer.MAX_VALUE), stack, treasureAllowed);
        if (!listCandidateEnchantments.isEmpty()) {
            Weighting.getRandom(random, listCandidateEnchantments).ifPresent(list::add);
            while (random.nextInt(50) <= power) {
                if (!list.isEmpty()) EnchantmentHelper.removeConflicts(listCandidateEnchantments, Util.getLast(list));
                if (listCandidateEnchantments.isEmpty()) break;

                EnchantmentLevelEntry candidate = Weighting.getRandom(random, listCandidateEnchantments).get();
                if(candidate != null && ( candidate.enchantment instanceof ProtectionEnchantment)) {
                    if(numProtectionEnchantments >= 2) {
                        listCandidateEnchantments.remove(candidate);
                        continue;
                    }
                    else numProtectionEnchantments += 1;
                }
                list.add(candidate);
                power /= 2;
            }
        }
        return list;
    }


    @Overwrite
    public static List<EnchantmentLevelEntry> getPossibleEntries(int power, ItemStack stack, boolean treasureAllowed) {

        // Get info related to the item to enchant (current enchantments)
        ArrayList<EnchantmentLevelEntry> listPossibleEntries = Lists.newArrayList();
        Map<Enchantment, Integer> currentEnchantments = EnchantmentHelper.get(stack);
        boolean isBook = stack.isOf(Items.BOOK) || stack.isOf(Items.ENCHANTED_BOOK);

        // Power is reduced if the item already has enchantments:
        int numProtectionEnchantments = 0;
        for (Enchantment enchantment : currentEnchantments.keySet()) {
            if(enchantment instanceof ProtectionEnchantment) numProtectionEnchantments += 1;
            power -= enchantment.getMinPower(currentEnchantments.get(enchantment))/10;
        }

        // Loop over every possible enchantment:
        forEachEnchant: for (Enchantment enchantment : Registries.ENCHANTMENT) {

            // Is the enchantment allowed on this item?
            if((!enchantment.isAcceptableItem(stack) && !isBook))
                continue forEachEnchant;

            if(((enchantment.isTreasure() && !treasureAllowed) || !enchantment.isAvailableForRandomSelection()) && !currentEnchantments.containsKey(enchantment))
                continue forEachEnchant;

            // Does the enchantment play nice with other enchantments already on the item?
            for (Enchantment currentEnchant : currentEnchantments.keySet()) {
                if(currentEnchant != enchantment && !currentEnchant.canCombine(enchantment)) continue forEachEnchant;
                if(enchantment instanceof ProtectionEnchantment && numProtectionEnchantments >= 2) continue forEachEnchant;
            }

            // Get the level of the new enchantment, and add it to the list:
            int currentLevel = currentEnchantments.containsKey(enchantment) ? currentEnchantments.get(enchantment) : 0;
            int minLevel = Math.max(currentLevel, enchantment.getMinLevel() - 1);
            for (int i = enchantment.getMaxLevel(); i > minLevel; --i) {

                // See if we can affort the new enchantment (or upgrade to old enchantment):
                int upgradeRebate = currentLevel == 0 ? 0 : (enchantment.getMinPower(currentLevel)) * 2/3;
                int overEnchantingCost = (enchantment instanceof ProtectionEnchantment) ? i * numProtectionEnchantments : 0;
                if (power + upgradeRebate < enchantment.getMinPower(i) + overEnchantingCost|| power > enchantment.getMaxPower(i) + overEnchantingCost) continue;

                // Add the enchantment and level to the list:
                listPossibleEntries.add(new EnchantmentLevelEntry(enchantment, i));
                continue forEachEnchant;
            }
        }
        return listPossibleEntries;
    }
}
