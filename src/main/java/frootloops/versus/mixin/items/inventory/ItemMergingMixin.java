package frootloops.versus.mixin.items.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(ScreenHandler.class)
public class ItemMergingMixin {

    @Shadow private StackReference getCursorStackReference() {
        return null;
    }

    private static final Map<Item, Object[]> CONVERSION_MAP = new HashMap<>();
    static {
        CONVERSION_MAP.put(Items.IRON_INGOT, new Object[]{Items.IRON_BLOCK, 9});
        CONVERSION_MAP.put(Items.IRON_NUGGET, new Object[]{Items.IRON_INGOT, 4});
        CONVERSION_MAP.put(Items.RAW_IRON, new Object[]{Items.RAW_IRON_BLOCK, 9});
        CONVERSION_MAP.put(Items.GOLD_INGOT, new Object[]{Items.GOLD_BLOCK, 9});
        CONVERSION_MAP.put(Items.GOLD_NUGGET, new Object[]{Items.GOLD_INGOT, 4});
        CONVERSION_MAP.put(Items.RAW_GOLD, new Object[]{Items.RAW_GOLD_BLOCK, 9});
        CONVERSION_MAP.put(Items.COPPER_INGOT, new Object[]{Items.COPPER_BLOCK, 9});
        CONVERSION_MAP.put(Items.RAW_COPPER, new Object[]{Items.RAW_COPPER_BLOCK, 9});
        CONVERSION_MAP.put(Items.EMERALD, new Object[]{Items.EMERALD_BLOCK, 9});
        CONVERSION_MAP.put(Items.COAL, new Object[]{Items.COAL_BLOCK, 9});
        CONVERSION_MAP.put(Items.DIAMOND, new Object[]{Items.DIAMOND_BLOCK, 9});
        CONVERSION_MAP.put(Items.LAPIS_LAZULI, new Object[]{Items.LAPIS_BLOCK, 9});
        CONVERSION_MAP.put(Items.WHEAT, new Object[]{Items.HAY_BLOCK, 9});
        CONVERSION_MAP.put(Items.CLAY_BALL, new Object[]{Items.CLAY, 9});
        CONVERSION_MAP.put(Items.SNOWBALL, new Object[]{Items.SNOW_BLOCK, 4});
        CONVERSION_MAP.put(Items.SLIME_BALL, new Object[]{Items.SLIME_BLOCK, 4});
    }

    @Overwrite
    private boolean handleSlotClick(PlayerEntity player, ClickType clickType, Slot slot, ItemStack stack, ItemStack cursorStack) {
        FeatureSet featureSet = player.getWorld().getEnabledFeatures();
        if (cursorStack.isItemEnabled(featureSet) && (cursorStack.onStackClicked(slot, clickType, player) || ItemMergingMixin.tryFuseWithStack(cursorStack, slot, clickType))) {
            return true;
        }
        return stack.isItemEnabled(featureSet) && stack.onClicked(cursorStack, slot, clickType, player, this.getCursorStackReference());
    }

    private static boolean tryFuseWithStack(ItemStack cursorStack, Slot slot, ClickType clickType) {
        if(slot == null || cursorStack.isEmpty() || !slot.hasStack()) return false;
        if(slot.getStack() == cursorStack) return false;
        if(!clickType.equals(ClickType.LEFT)) return false;

        Item itemToMergeInto;
        int amountRequired, amountBetweenBoth;

        Item cursorItem = cursorStack.getItem();
        if(CONVERSION_MAP.containsKey(cursorItem)) {
            Object[] conversionResult = CONVERSION_MAP.get(cursorItem);
            itemToMergeInto = (Item)conversionResult[0];
            amountRequired = (int)conversionResult[1];
        }
        else if(cursorStack.isDamaged() && slot.getStack().isDamaged()) {
            if(cursorItem != slot.getStack().getItem()) return false;
            if(cursorStack.hasEnchantments() || slot.getStack().hasEnchantments()) return false;
            if(!cursorStack.getName().getString().equals(slot.getStack().getName().getString())) return false;

            int maxUses = cursorStack.getMaxDamage();
            int damage =  cursorStack.getDamage() + slot.getStack().getDamage() - maxUses - maxUses/10;
            if(damage < -maxUses/4) return false;
            slot.getStack().setDamage(Math.max(0, damage));
            cursorStack.setCount(0);
            return true;
        }
        else return false;

        int minAmount = 999;
        if(cursorStack.isOf(slot.getStack().getItem())) {
            amountBetweenBoth = cursorStack.getCount() + slot.getStack().getCount();
            minAmount = cursorStack.getMaxCount() + 1;
        }
        else if(itemToMergeInto != null && slot.getStack().isOf(itemToMergeInto)) {
            amountBetweenBoth = cursorStack.getCount();
            minAmount = amountRequired;
        }
        else return false;

        if(itemToMergeInto != null && amountBetweenBoth >= minAmount) {
            int numPossibleCrafts = amountBetweenBoth/amountRequired;
            int numOutputAlreadyThere = slot.getStack().isOf(itemToMergeInto) ? slot.getStack().getCount() : 0;
            int numOutput = Math.min(numPossibleCrafts + numOutputAlreadyThere, 64);
            int numLeftover = amountBetweenBoth % amountRequired + Math.max(0, (numPossibleCrafts + numOutputAlreadyThere - numOutput) * amountRequired);
            cursorStack.setCount(numLeftover);
            slot.setStack(new ItemStack(itemToMergeInto, numOutput));
            return true;
        }
        return false;
    }
}
