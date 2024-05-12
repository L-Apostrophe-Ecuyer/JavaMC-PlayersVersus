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

@Mixin(ScreenHandler.class)
public class ItemMergingMixin {

    @Shadow private StackReference getCursorStackReference() {
        return null;
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

        // TODO: Make this a dict lookup
        Item cursorItem = cursorStack.getItem();
        if(cursorItem == Items.IRON_INGOT) {
            itemToMergeInto = Items.IRON_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.RAW_IRON) {
            itemToMergeInto = Items.RAW_IRON_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.IRON_NUGGET) {
            itemToMergeInto = Items.IRON_INGOT;
            amountRequired = 4;
        }
        else if(cursorItem == Items.GOLD_INGOT) {
            itemToMergeInto = Items.GOLD_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.RAW_GOLD) {
            itemToMergeInto = Items.RAW_GOLD_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.GOLD_NUGGET) {
            itemToMergeInto = Items.GOLD_INGOT;
            amountRequired = 4;
        }
        else if(cursorItem == Items.COPPER_INGOT) {
            itemToMergeInto = Items.COPPER_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.RAW_COPPER) {
            itemToMergeInto = Items.RAW_COPPER_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.EMERALD) {
            itemToMergeInto = Items.EMERALD_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.COAL) {
            itemToMergeInto = Items.COAL_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.DIAMOND) {
            itemToMergeInto = Items.DIAMOND_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.LAPIS_LAZULI) {
            itemToMergeInto = Items.LAPIS_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.WHEAT) {
            itemToMergeInto = Items.HAY_BLOCK;
            amountRequired = 9;
        }
        else if(cursorItem == Items.CLAY_BALL) {
            itemToMergeInto = Items.CLAY;
            amountRequired = 9;
        }
        else if(cursorItem == Items.SNOWBALL) {
            itemToMergeInto = Items.SNOW_BLOCK;
            amountRequired = 4;
        }
        else if(cursorItem == Items.SLIME_BALL) {
            itemToMergeInto = Items.SLIME_BLOCK;
            amountRequired = 4;
        }
        else if(cursorStack.isDamaged() && slot.getStack().isDamaged()) {
            if(cursorItem != slot.getStack().getItem()) return false;
            if(cursorStack.hasEnchantments() || slot.getStack().hasEnchantments()) return false;
            if(!cursorStack.getName().getString().equals(slot.getStack().getName().getString())) return false;

            int maxUses = cursorStack.getMaxDamage();
            int damage = Math.max(0, cursorStack.getDamage() + slot.getStack().getDamage() - maxUses - maxUses/10);
            slot.getStack().setDamage(damage);
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
