package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerMenu.class)
public class ItemMergingMixin {

    @Shadow private SlotAccess createCarriedSlotAccess() {
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
        CONVERSION_MAP.put(Items.COPPER_INGOT, new Object[]{Items.COPPER_BLOCK.weathering().unaffected(), 9});
        CONVERSION_MAP.put(Items.COPPER_NUGGET, new Object[]{Items.COPPER_INGOT, 4});
        CONVERSION_MAP.put(Items.RAW_COPPER, new Object[]{Items.RAW_COPPER_BLOCK, 9});
        CONVERSION_MAP.put(Items.EMERALD, new Object[]{Items.EMERALD_BLOCK, 9});
        CONVERSION_MAP.put(Items.COAL, new Object[]{Items.COAL_BLOCK, 9});
        CONVERSION_MAP.put(Items.DIAMOND, new Object[]{Items.DIAMOND_BLOCK, 9});
        CONVERSION_MAP.put(Items.LAPIS_LAZULI, new Object[]{Items.LAPIS_BLOCK, 9});
        CONVERSION_MAP.put(Items.WHEAT, new Object[]{Items.HAY_BLOCK, 9});
        CONVERSION_MAP.put(Items.CLAY_BALL, new Object[]{CustomBlockItems.GRAY_CLAY, 9});
        CONVERSION_MAP.put(CustomBlockItems.BROWN_CLAY_BALL, new Object[]{CustomBlockItems.BROWN_CLAY, 9});
        CONVERSION_MAP.put(Items.SNOWBALL, new Object[]{Items.SNOW_BLOCK, 4});
        CONVERSION_MAP.put(Items.SLIME_BALL, new Object[]{Items.SLIME_BLOCK, 4});
    }

    @Overwrite
    private boolean tryItemClickBehaviourOverride(Player player, ClickAction clickType, Slot slot, ItemStack stack, ItemStack cursorStack) {
        FeatureFlagSet featureSet = player.level().enabledFeatures();
        if (cursorStack.isItemEnabled(featureSet) && (cursorStack.overrideStackedOnOther(slot, clickType, player) || ItemMergingMixin.tryFuseWithStack(cursorStack, slot, clickType))) {
            return true;
        }
        return stack.isItemEnabled(featureSet) && stack.overrideOtherStackedOnMe(cursorStack, slot, clickType, player, this.createCarriedSlotAccess());
    }

    private static boolean tryFuseWithStack(ItemStack cursorStack, Slot slot, ClickAction clickType) {
        if(slot == null || cursorStack.isEmpty() || !slot.hasItem()) return false;
        if(slot.getItem() == cursorStack) return false;
        if(!clickType.equals(ClickAction.PRIMARY)) return false;

        Item itemToMergeInto;
        int amountRequired, amountBetweenBoth;

        Item cursorItem = cursorStack.getItem();
        if(CONVERSION_MAP.containsKey(cursorItem)) {
            Object[] conversionResult = CONVERSION_MAP.get(cursorItem);
            itemToMergeInto = (Item)conversionResult[0];
            amountRequired = (int)conversionResult[1];
        }
        else if(cursorStack.isDamaged() && slot.getItem().isDamaged()) {
            if(cursorItem != slot.getItem().getItem()) return false;
            if(cursorStack.isEnchanted() || slot.getItem().isEnchanted()) return false;
            if(!cursorStack.getHoverName().getString().equals(slot.getItem().getHoverName().getString())) return false;

            int maxUses = cursorStack.getMaxDamage();
            int damage =  cursorStack.getDamageValue() + slot.getItem().getDamageValue() - maxUses - maxUses/10;
            if(damage < -maxUses/4) return false;
            slot.getItem().setDamageValue(Math.max(0, damage));
            cursorStack.setCount(0);
            return true;
        }
        else return false;

        int minAmount = 999;
        if(cursorStack.is(slot.getItem().getItem())) {
            amountBetweenBoth = cursorStack.getCount() + slot.getItem().getCount();
            minAmount = cursorStack.getMaxStackSize() + 1;
        }
        else if(itemToMergeInto != null && slot.getItem().is(itemToMergeInto)) {
            amountBetweenBoth = cursorStack.getCount();
            minAmount = amountRequired;
        }
        else return false;

        if(itemToMergeInto != null && amountBetweenBoth >= minAmount) {
            int numPossibleCrafts = amountBetweenBoth/amountRequired;
            int numOutputAlreadyThere = slot.getItem().is(itemToMergeInto) ? slot.getItem().getCount() : 0;
            int numOutput = Math.min(numPossibleCrafts + numOutputAlreadyThere, 64);
            int numLeftover = amountBetweenBoth % amountRequired + Math.max(0, (numPossibleCrafts + numOutputAlreadyThere - numOutput) * amountRequired);
            cursorStack.setCount(numLeftover);
            slot.setByPlayer(new ItemStack(itemToMergeInto, numOutput));
            return true;
        }
        return false;
    }
}
