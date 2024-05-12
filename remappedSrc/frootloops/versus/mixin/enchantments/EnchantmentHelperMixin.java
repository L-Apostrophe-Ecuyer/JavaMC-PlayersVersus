package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.Enchants;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static frootloops.versus.mod.enchantments.Enchants.MAX_PROTECTION_LEVELS_PER_ITEM;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    private static int getProtectionLevelsOf(ItemStack stack) {
        if(!stack.hasEnchantments()) return 0;
        MutableInt protectionLevels = new MutableInt(0);
        forEachEnchantment((Enchantment stackEnchantment, int stackLevel) -> protectionLevels.add(stackEnchantment instanceof ProtectionEnchantment ? stackLevel : 0), stack);
        return protectionLevels.intValue();
    }

    private static int getNumProtectionEnchantsOf(ItemStack stack) {
        if(!stack.hasEnchantments()) return 0;
        MutableInt protectionEnchants = new MutableInt(0);
        forEachEnchantment((Enchantment stackEnchantment, int stackLevel) -> protectionEnchants.add(stackEnchantment instanceof ProtectionEnchantment ? 1 : 0), stack);
        return protectionEnchants.intValue();
    }

    @Overwrite
    public static List<EnchantmentLevelEntry> generateEnchantments(FeatureSet enabledFeatures, Random random, ItemStack stack, int level, boolean treasureAllowed) {
        ArrayList<EnchantmentLevelEntry> selectedEnchantments = Lists.newArrayList();
        Item item = stack.getItem();
        int itemEnchantability = item.getEnchantability();
        if (itemEnchantability <= 0) {
            return selectedEnchantments;
        }

        level += 1 + random.nextInt(itemEnchantability / 4 + 1) + random.nextInt(itemEnchantability / 4 + 1);
        float randomFloat = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        List<EnchantmentLevelEntry> possibleEnchantments = getPossibleEntries(enabledFeatures, level = MathHelper.clamp(Math.round((float)level + (float)level * randomFloat), 1, Integer.MAX_VALUE), stack, treasureAllowed);

        int numProtectionLevels = getProtectionLevelsOf(stack);
        int numProtectionEnchants = numProtectionLevels > 1 ? getNumProtectionEnchantsOf(stack) : numProtectionLevels;

        if (!possibleEnchantments.isEmpty()) {
            Weighting.getRandom(random, possibleEnchantments).ifPresent(selectedEnchantments::add);
            while (random.nextInt(50) <= level) {

                // Make sure not to add too many protection enchantments:
                EnchantmentLevelEntry enchantmentChosen = Util.getLast(selectedEnchantments);
                if(enchantmentChosen.enchantment instanceof ProtectionEnchantment && (numProtectionEnchants >= 2 || enchantmentChosen.level + numProtectionLevels > MAX_PROTECTION_LEVELS_PER_ITEM)) {
                    selectedEnchantments.remove(selectedEnchantments.size() - 1);
                    possibleEnchantments.remove(enchantmentChosen);
                }

                if (!selectedEnchantments.isEmpty()) {
                    EnchantmentHelper.removeConflicts(possibleEnchantments, Util.getLast(selectedEnchantments));
                }

                if (possibleEnchantments.isEmpty()) break;
                Weighting.getRandom(random, possibleEnchantments).ifPresent(selectedEnchantments::add);
                level /= 2;
            }
        }
        return selectedEnchantments;
    }


    @Overwrite
    public static List<EnchantmentLevelEntry> getPossibleEntries(FeatureSet enabledFeatures, int level, ItemStack stack, boolean treasureAllowed) {
        Enchantment e;

        // Get info related to the item to enchant (current enchantments)
        ArrayList<EnchantmentLevelEntry> listPossibleEntries = Lists.newArrayList();
        ItemEnchantmentsComponent currentEnchantments = stack.getEnchantments();
        boolean isBook = stack.isOf(Items.BOOK) || stack.isOf(Items.ENCHANTED_BOOK);

        // Power is reduced if the item already has enchantments:
        for (RegistryEntry<Enchantment> enchantmentRegistryEntry : currentEnchantments.getEnchantments()) {
            e = enchantmentRegistryEntry.value();
            level -= e.getMinPower(currentEnchantments.getLevel(e))/10;
        }

        // Loop over every possible enchantment:
        int numProtectionLevels = getProtectionLevelsOf(stack);
        int numProtectionEnchants = numProtectionLevels > 1 ? getNumProtectionEnchantsOf(stack) : numProtectionLevels;
        forEachEnchant: for (Enchantment enchantment : Registries.ENCHANTMENT) {

            // Is the enchantment allowed on this item?
            if((!enchantment.isAcceptableItem(stack) && !isBook))
                continue forEachEnchant;

            if(((enchantment.isTreasure() && !treasureAllowed) || !enchantment.isAvailableForRandomSelection()) && (currentEnchantments.getLevel(enchantment) <= 0))
                continue forEachEnchant;

            // Does the enchantment play nice with other enchantments already on the item?
            for (RegistryEntry<Enchantment> enchantmentRegistryEntry : currentEnchantments.getEnchantments()) {
                e = enchantmentRegistryEntry.value();
                if(e != enchantment && !e.canCombine(enchantment)) continue forEachEnchant;
                if(enchantment instanceof ProtectionEnchantment && (numProtectionEnchants >= 2 || numProtectionLevels >= MAX_PROTECTION_LEVELS_PER_ITEM)) continue forEachEnchant;
            }

            // Get the max possible level of the new enchantment, and add it to the list:
            int currentLevel = currentEnchantments.getLevel(enchantment);
            int minLevel = Math.max(currentLevel, enchantment.getMinLevel() - 1);
            for (int i = enchantment.getMaxLevel(); i > minLevel; --i) {

                // See if it reaches the max amount of protection enchants:
                if(enchantment instanceof ProtectionEnchantment && numProtectionLevels + i > MAX_PROTECTION_LEVELS_PER_ITEM) continue;

                // See if we can affort the new enchantment (or upgrade to old enchantment):
                int upgradeRebate = currentLevel == 0 ? 0 : (enchantment.getMinPower(currentLevel)) * 2/3;
                int overEnchantingCost = (enchantment instanceof ProtectionEnchantment) ? i * numProtectionLevels : 0;
                if (level + upgradeRebate < enchantment.getMinPower(i) + overEnchantingCost|| level > enchantment.getMaxPower(i) + overEnchantingCost) continue;

                // Add the enchantment and level to the list:
                listPossibleEntries.add(new EnchantmentLevelEntry(enchantment, i));
                continue forEachEnchant;
            }
        }
        return listPossibleEntries;
    }

    @FunctionalInterface
    static interface Consumer {
        public void accept(Enchantment var1, int var2);
    }

    private static void forEachEnchantment(Consumer consumer, ItemStack stack) {
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentsMap()) {
            consumer.accept((Enchantment)((RegistryEntry)entry.getKey()).value(), entry.getIntValue());
        }
    }
}
