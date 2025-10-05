package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EnchantableComponent;
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
    public static List<EnchantmentLevelEntry> getPossibleEntries(int enchPower, ItemStack stack, Stream<RegistryEntry<Enchantment>> possibleEnchantments) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
        boolean isBook = stack.isOf(Items.BOOK);

        final int itemEnchPower;
        EnchantableComponent enchantableComponent = stack.get(DataComponentTypes.ENCHANTABLE);
        if(enchantableComponent != null && !(isBook || stack.isOf(Items.FISHING_ROD) || stack.isOf(Items.TRIDENT))) {
            if(enchantableComponent.value() == 1) itemEnchPower = Math.clamp(enchPower/3, 1, 10);
            else if(enchantableComponent.value() <= 4) itemEnchPower = Math.max(1, enchPower - 4 + enchantableComponent.value());
            else itemEnchPower = enchPower + enchantableComponent.value()/3;
        }
        else itemEnchPower = enchPower;

        possibleEnchantments.filter(enchantment -> ((enchantment.value()).isPrimaryItem(stack) || isBook)).forEach(enchantmentRegistryEntry -> {
            boolean canApplyEnchanment = true;
            int currentLevel = 0;
            if(!itemEnchantmentsComponent.isEmpty()) {
                for (RegistryEntry<Enchantment> itemEnchantment : itemEnchantmentsComponent.getEnchantments()) {
                    if(itemEnchantment.equals(enchantmentRegistryEntry)) {
                        currentLevel = EnchantRegistryHelper.getLevel(stack, enchantmentRegistryEntry);
                        break;
                    }else if(!Enchantment.canBeCombined(enchantmentRegistryEntry, itemEnchantment)) {
                        canApplyEnchanment = false;
                        break;
                    }
                }
            }
            if(canApplyEnchanment) {
                Enchantment enchantmentToAdd = enchantmentRegistryEntry.value();
                int currentPow = currentLevel <= 0 ? 0 : enchantmentToAdd.getMinPower(currentLevel) / 6;
                if(currentLevel < enchantmentToAdd.getMaxLevel()) {
                    for (int lvl = enchantmentToAdd.getMaxLevel(); lvl >= Math.max(currentLevel + 1, enchantmentToAdd.getMinLevel()); --lvl) {
                        int minPower = enchantmentToAdd.getMinPower(lvl) - currentPow;
                        int maxPower = enchantmentToAdd.getMaxPower(lvl);
                        if (itemEnchPower < minPower || itemEnchPower > maxPower) continue;
                        list.add(new EnchantmentLevelEntry(enchantmentRegistryEntry, lvl));
                        break;
                    }
                }
            }
        });;

        return list;
    }
}
