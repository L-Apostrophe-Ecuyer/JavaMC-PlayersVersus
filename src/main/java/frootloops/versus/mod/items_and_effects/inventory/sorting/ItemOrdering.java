package frootloops.versus.mod.items_and_effects.inventory.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.EmptyBlockGetter;

/**
 * Which of two stacks to sort first.
 */
public final class ItemOrdering {

    private ItemOrdering() {
    }


    public static boolean shouldGoBefore(ItemSlot slot, ItemSlot slotToCompareTo) {
        return shouldGoBefore(slot, slotToCompareTo, false, false, false);
    }

    public static boolean shouldGoBefore(ItemSlot slot, ItemSlot slotToCompareTo, boolean skipNonTools) {
        return shouldGoBefore(slot, slotToCompareTo, skipNonTools, skipNonTools, false);
    }

    public static boolean shouldGoBefore(ItemSlot slot, ItemSlot slotToCompareTo, boolean skipNonTools, boolean skipBlocks, boolean compareNames) {
        if(slotToCompareTo == null) return true;
        if(slot == null) return false;
        if(slot.itemType() != slotToCompareTo.itemType() && slot.itemType() == ItemType.TRASH) return false;
        if(slot.itemType() != slotToCompareTo.itemType() && slotToCompareTo.itemType() == ItemType.TRASH) return true;
        if(ItemStack.isSameItemSameComponents(slot.stack(), slotToCompareTo.stack())) return slot.stack().getCount() > slotToCompareTo.stack().getCount();
        if(skipNonTools) {
            if(slot.isToolOrWeapon() && !slotToCompareTo.isToolOrWeapon()) return true;
            if(!slot.isToolOrWeapon() && slotToCompareTo.isToolOrWeapon()) return false;
        }
        int itemComparaison = compareItemType(slot.itemType(), slotToCompareTo.itemType());
        if(skipBlocks && slot.isBlock() && slotToCompareTo.isBlock()) itemComparaison = 0;
        if(itemComparaison != 0) return itemComparaison < 0 ? true : false;

        // Everything below is for stacks having the same type:
        if(slot.stack().getRarity().ordinal() > slotToCompareTo.stack().getRarity().ordinal()) return true;

        // Armor and weapon comparaison:
        if(slot.isToolOrWeapon()) {
            double modifiersOfStack = getToolPreferenceValue(slot.stack());
            double modifiersOfOther = getToolPreferenceValue(slotToCompareTo.stack());
            return modifiersOfStack > modifiersOfOther;
        }
        if(slot.isArmor()) {
            EquipmentSlot equipmentSlot = getPreferredEquipmentSlotOf(slot.itemType());
            double modifiersOfStack = getArmorPreferenceValue(slot.stack(), equipmentSlot);
            double modifiersOfOther = getArmorPreferenceValue(slotToCompareTo.stack(), equipmentSlot);
            return modifiersOfStack > modifiersOfOther;
        }
        else if(skipNonTools) {
            return false;
        }

        // Food special comparaison:
        else if(slot.isFood()) {
            if(slot.stack().hasFoil() && !slotToCompareTo.stack().hasFoil()) return true;
            if(slot.stack().has(DataComponents.USE_REMAINDER)  && !slotToCompareTo.stack().has(DataComponents.USE_REMAINDER) ) return false;

            FoodProperties food = slot.stack().getComponents().getOrDefault(DataComponents.FOOD, null);
            FoodProperties otherFood = slotToCompareTo.stack().getComponents().getOrDefault(DataComponents.FOOD, null);
            boolean isNewerFoodBetter = (food.nutrition() + 3F * food.saturation()) >= (otherFood.nutrition() + 3F * otherFood.saturation());
            if(isNewerFoodBetter) return true;
            else if(food.saturation() == otherFood.saturation() && food.nutrition() == otherFood.nutrition() && slot.stack().getCount() > slotToCompareTo.stack().getCount()) return true;
            else return false;
        }

        // Potion comparaison:
        else if(slot.itemType() == ItemType.POTIONS) {
            if(slot.stack().getMaxStackSize() > slotToCompareTo.stack().getMaxStackSize()) return true;
            if(slot.stack().is(Items.POTION) && !slotToCompareTo.stack().is(Items.POTION)) return true;

            PotionContents potion = slot.stack().getComponents().getOrDefault(DataComponents.POTION_CONTENTS, null);
            if(potion == null || !potion.hasEffects()) return false;
            PotionContents otherPotion = slotToCompareTo.stack().getComponents().getOrDefault(DataComponents.POTION_CONTENTS, null);
            if(otherPotion == null || !potion.hasEffects()) return true;
            return (potion.getColor() <= otherPotion.getColor());
        }

        // Blocks comparaison:
        else if(slot.isBlock() && slot.stack().getItem() instanceof BlockItem blockItem && slot.stack().getItem() instanceof BlockItem otherBlockItem) {
            if(skipBlocks) return false;
            if(blockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && blockItem.getBlock().defaultDestroyTime() >= 1.0f) {
                if(otherBlockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && otherBlockItem.getBlock().defaultDestroyTime() >= 1.0f) {
                    return slot.toString().compareTo(slotToCompareTo.toString()) < 1;
                }
                else return true;
            }
            if(otherBlockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)) return false;
            if(blockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                if(otherBlockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                    if(ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_CORAL.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_CORAL.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    return blockItem.getBlock().defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col < otherBlockItem.getBlock().defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
                }
                else return true;
            }
            if(otherBlockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) return false;
            return blockItem.getBlock().defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col < otherBlockItem.getBlock().defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
        }

        // Clutch items:
        if(slot.itemType() == ItemType.CLUTCH_TOOL) {
            if(slot.stack().is(Items.ENDER_PEARL)) return true;
            if(slot.stack().is(Items.WIND_CHARGE)) return true;
            if(slot.stack().is(Items.WATER_BUCKET)) return true;
        }

        if(!compareNames) return false;
        return slot.toString().compareTo(slotToCompareTo.toString()) < 1;
    }

    /**
     * Compares two items' types.
     * @return -1 if stack's type is placed lower (better), 0 if the same, 1 if higher (should go further down)
     */
    public static int compareItemType(ItemType one, ItemType two) {
        return one.compareTo(two);
    }

    public static EquipmentSlot getPreferredEquipmentSlotOf(ItemType type) {
        return switch (type) {
            case SHIELD, TOTEMS -> EquipmentSlot.OFFHAND;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            case HELMET -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    private static double getArmorPreferenceValue(ItemStack stack, EquipmentSlot slot) {
        double attributeValue = ItemClassifier.attributeTotal(stack, slot);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamageValue())/64.0;
        return attributeValue + durabilityPoints;
    }

    private static double getToolPreferenceValue(ItemStack stack) {
        double attributeValue = ItemClassifier.attributeTotal(stack, EquipmentSlot.MAINHAND);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamageValue())/32.0;
        return attributeValue + durabilityPoints;
    }
}
