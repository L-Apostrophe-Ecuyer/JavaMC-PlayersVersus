package frootloops.versus.mixin.enchantments;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Overwrite
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int enchPower, ItemStack stack, Stream<Holder<Enchantment>> possibleEnchantments) {
        ArrayList<EnchantmentInstance> list = Lists.newArrayList();
        ItemEnchantments itemEnchantmentsComponent = stack.get(DataComponents.ENCHANTMENTS);
        boolean isBook = stack.is(Items.BOOK);

        final int itemEnchPower;
        Enchantable enchantableComponent = stack.get(DataComponents.ENCHANTABLE);
        if(enchantableComponent != null && !(isBook || stack.is(Items.FISHING_ROD) || stack.is(Items.TRIDENT))) {
            if(enchantableComponent.value() == 1) itemEnchPower = Math.clamp(enchPower/3, 1, 10);
            else if(enchantableComponent.value() <= 4) itemEnchPower = Math.max(1, enchPower - 4 + enchantableComponent.value());
            else itemEnchPower = enchPower + enchantableComponent.value()/3;
        }
        else itemEnchPower = enchPower;

        possibleEnchantments.filter(enchantment -> ((enchantment.value()).isPrimaryItem(stack) || isBook)).forEach(enchantmentRegistryEntry -> {
            boolean canApplyEnchanment = true;
            int currentLevel = 0;
            if(!itemEnchantmentsComponent.isEmpty()) {
                for (Holder<Enchantment> itemEnchantment : itemEnchantmentsComponent.keySet()) {
                    if(itemEnchantment.equals(enchantmentRegistryEntry)) {
                        currentLevel = EnchantRegistryHelper.getLevel(stack, enchantmentRegistryEntry);
                        break;
                    }else if(!Enchantment.areCompatible(enchantmentRegistryEntry, itemEnchantment)) {
                        canApplyEnchanment = false;
                        break;
                    }
                }
            }
            if(canApplyEnchanment) {
                Enchantment enchantmentToAdd = enchantmentRegistryEntry.value();
                int currentPow = currentLevel <= 0 ? 0 : enchantmentToAdd.getMinCost(currentLevel) / 6;
                if(currentLevel < enchantmentToAdd.getMaxLevel()) {
                    for (int lvl = enchantmentToAdd.getMaxLevel(); lvl >= Math.max(currentLevel + 1, enchantmentToAdd.getMinLevel()); --lvl) {
                        int minPower = enchantmentToAdd.getMinCost(lvl) - currentPow;
                        int maxPower = enchantmentToAdd.getMaxCost(lvl);
                        if (itemEnchPower < minPower || itemEnchPower > maxPower) continue;
                        list.add(new EnchantmentInstance(enchantmentRegistryEntry, lvl));
                        break;
                    }
                }
            }
        });;

        return list;
    }
}
