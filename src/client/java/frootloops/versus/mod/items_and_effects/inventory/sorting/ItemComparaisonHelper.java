package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.mod.items_and_effects.brewing.ConcentrateItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WallBlock;

public abstract class ItemComparaisonHelper {

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
                    return blockItem.getBlock().defaultBlockState().getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col < otherBlockItem.getBlock().defaultBlockState().getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col;
                }
                else return true;
            }
            if(otherBlockItem.getBlock().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) return false;
            return blockItem.getBlock().defaultBlockState().getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col < otherBlockItem.getBlock().defaultBlockState().getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col;
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

    public static ItemType getItemTypeOf(ItemStack stack) {
        if(stack.is(Items.PUFFERFISH) || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE) || stack.is(Items.POISONOUS_POTATO) || stack.is(Items.INK_SAC) || stack.is(Items.GLOW_INK_SAC)) return ItemType.TRASH;
        if((stack.getComponents().has(DataComponents.TOOL) || stack.getMaxDamage() > 0) && !stack.getComponents().has(DataComponents.EQUIPPABLE)) {
            if(stack.is(ItemTags.SWORDS)) return ItemType.SWORD;
            else if(stack.is(ItemTags.PICKAXES)) return ItemType.PICKAXE;
            else if(stack.is(ItemTags.AXES)) return ItemType.AXE;
            else if(stack.is(ItemTags.SHOVELS)) return ItemType.SHOVEL;
            else if(stack.is(ItemTags.HOES)) return ItemType.HOE;
            else if(stack.is(ItemTags.BOW_ENCHANTABLE) && !stack.is(Items.CROSSBOW)) return ItemType.BOW;
            else if(stack.is(ItemTags.CROSSBOW_ENCHANTABLE) || stack.is(Items.CROSSBOW)) return ItemType.CROSSBOW;
            else if(stack.getUseAnimation() == ItemUseAnimation.BLOCK || stack.is(Items.SHIELD)) return ItemType.SHIELD;
            else if(stack.is(Items.SHEARS)) return ItemType.SHEARS;
            else if(stack.is(Items.FISHING_ROD)) return ItemType.FISHING_ROD;
            else if(stack.is(Items.BRUSH)) return ItemType.MISC_TOOL;
            else if(getAttributeValueWithStack(stack, EquipmentSlot.MAINHAND) > 1.0 || stack.is(ItemTags.WEAPON_ENCHANTABLE))
                return ItemType.SPECIAL_WEAPON;
            else return ItemType.MISC_TOOL;
        }
        else if(stack.getComponents().has(DataComponents.EQUIPPABLE)) {
            Equippable equipComponent = stack.getComponents().get(DataComponents.EQUIPPABLE);
            if(!equipComponent.canBeEquippedBy(EntityTypes.PLAYER)) return ItemType.MISC_TOOL;
            else if(stack.is(Items.ELYTRA)) return ItemType.ELYTRA;
            else if(equipComponent.slot() == EquipmentSlot.CHEST) return ItemType.CHESTPLATE;
            else if(equipComponent.slot() == EquipmentSlot.LEGS) return ItemType.LEGGINGS;
            else if(equipComponent.slot() == EquipmentSlot.FEET) return ItemType.BOOTS;
            else if(equipComponent.slot() == EquipmentSlot.HEAD) return ItemType.HELMET;
            else return ItemType.MISC_TOOL;
        }
        else if(stack.getComponents().has(DataComponents.DEATH_PROTECTION)) return ItemType.TOTEMS;
        else if(stack.is(ItemTags.ARROWS)) return ItemType.ARROWS;
        else if(stack.is(Items.WATER_BUCKET) || stack.is(Items.ENDER_PEARL) || stack.is(Items.WIND_CHARGE)) return ItemType.CLUTCH_TOOL;
        else if(stack.is(Items.END_CRYSTAL) || stack.is(Items.ENDER_PEARL) || stack.is(Items.COBWEB) || stack.is(Items.SNOWBALL) || stack.is(Items.FIRE_CHARGE)) return ItemType.COMBAT_ITEMS;
        else if(stack.is(Items.SPYGLASS)) return ItemType.SPYGLASS;
        else if(stack.getItem() instanceof SmithingTemplateItem) return ItemType.SMITHING_TEMPLATE;
        else if(stack.getComponents().has(DataComponents.INSTRUMENT)) return ItemType.MISC_TOOL;
        else if(stack.is(Items.MAP) || stack.is(Items.FILLED_MAP)) return ItemType.MISC_TOOL;
        else if(stack.is(Items.BUCKET) || stack.getRecipeRemainder() == Items.BUCKET.getDefaultInstance()) return ItemType.MISC_TOOL;
        else if(Minecraft.getInstance().level.potionBrewing().isPotionIngredient(stack)  || stack.getItem() instanceof ConcentrateItem) return ItemType.BREWING_INGREDIENT;
        else if(stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return ItemType.GAPPLES;
        else if(stack.getComponents().has(DataComponents.FOOD)) return ItemType.FOOD;
        else if(stack.is(ItemTags.SHULKER_BOXES)) return ItemType.SHULKER_BOX;
        else if(stack.getComponents().has(DataComponents.BUNDLE_CONTENTS)) return ItemType.BUNDLE;
        else if(stack.getComponents().has(DataComponents.CONTAINER)) return ItemType.ITEM_CONTAINER;
        else if(stack.getComponents().has(DataComponents.CONTAINER_LOOT)) return ItemType.ITEM_CONTAINER;
        else if(stack.getComponents().has(DataComponents.POTION_CONTENTS)) {
            return stack.get(DataComponents.POTION_CONTENTS).hasEffects() ? ItemType.POTIONS : ItemType.MISC;
        }
        else if(stack.getUseAnimation() != ItemUseAnimation.NONE || stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN)) return ItemType.TORCHES_AND_LANTERNS;
        else if(stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if(block instanceof CraftingTableBlock || block instanceof BaseEntityBlock || block.defaultBlockState().is(BlockTags.ANVIL)) return ItemType.BLOCK_WORKSTATION;
            if(block.defaultDestroyTime() > 32F && block.getExplosionResistance() > 128F) return ItemType.BLOCK_OTHER;
            if(block.defaultBlockState().isSolidRender()) return ItemType.BLOCK_FULL;
            else if(block.defaultBlockState().getLightEmission() > 4) return ItemType.MISC_TOOL;
            else if(block instanceof SlabBlock) return ItemType.BLOCK_SLAB;
            else if(block instanceof StairBlock) return ItemType.BLOCK_STAIRS;
            else if(block instanceof WallBlock) return ItemType.BLOCK_WALL;
            else if(block instanceof FenceBlock) return ItemType.BLOCK_FENCE;
            else if(block instanceof VegetationBlock || (block.defaultDestroyTime() < 1F && block.defaultBlockState().getSoundType() == SoundType.GRASS)) return ItemType.PLANTS_AND_FLOWERS;
            else return ItemType.BLOCK_OTHER;
        }
        else if(stack.getOrDefault(DataComponents.BANNER_PATTERNS, null) != null) return ItemType.BANNER_PATTERNS;
        else if(stack.getItem().getDescriptionId().endsWith("pottery_sherd")) return ItemType.POTTERY;
        else if(stack.is(Items.BRICK)) return ItemType.POTTERY;
        else if(stack.is(Items.FLOWER_POT)) return ItemType.POTTERY;
        else if(stack.is(Items.DISC_FRAGMENT_5)) return ItemType.DISCS;
        else if(stack.getOrDefault(DataComponents.JUKEBOX_PLAYABLE, null) != null) return ItemType.DISCS;
        else return ItemType.MISC;
    }

    private static double getAttributeValueWithStack(ItemStack stack, EquipmentSlot slot) {
        ItemAttributeModifiers attributeModifiersComponent = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return attributeModifiersComponent.compute(0.0, slot);
    }

    private static double getArmorPreferenceValue(ItemStack stack, EquipmentSlot slot) {
        double attributeValue = getAttributeValueWithStack(stack, slot);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamageValue())/64.0;
        return attributeValue + durabilityPoints;
    }

    private static double getToolPreferenceValue(ItemStack stack) {
        double attributeValue = getAttributeValueWithStack(stack, EquipmentSlot.MAINHAND);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamageValue())/32.0;
        return attributeValue + durabilityPoints;
    }
}
