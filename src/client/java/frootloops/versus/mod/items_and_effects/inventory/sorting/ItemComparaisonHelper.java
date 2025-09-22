package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.VersusMod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;

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
        if(ItemStack.areItemsAndComponentsEqual(slot.stack(), slotToCompareTo.stack())) return slot.stack().getCount() > slotToCompareTo.stack().getCount();
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
            if(slot.stack().hasGlint() && !slotToCompareTo.stack().hasGlint()) return true;
            if(slot.stack().contains(DataComponentTypes.USE_REMAINDER)  && !slotToCompareTo.stack().contains(DataComponentTypes.USE_REMAINDER) ) return false;

            FoodComponent food = slot.stack().getComponents().getOrDefault(DataComponentTypes.FOOD, null);
            FoodComponent otherFood = slotToCompareTo.stack().getComponents().getOrDefault(DataComponentTypes.FOOD, null);
            boolean isNewerFoodBetter = (food.nutrition() + 3F * food.saturation()) >= (otherFood.nutrition() + 3F * otherFood.saturation());
            if(isNewerFoodBetter) return true;
            else if(food.saturation() == otherFood.saturation() && food.nutrition() == otherFood.nutrition() && slot.stack().getCount() > slotToCompareTo.stack().getCount()) return true;
            else return false;
        }

        // Potion comparaison:
        else if(slot.itemType() == ItemType.POTIONS) {
            if(slot.stack().getMaxCount() > slotToCompareTo.stack().getMaxCount()) return true;
            if(slot.stack().isOf(Items.POTION) && !slotToCompareTo.stack().isOf(Items.POTION)) return true;

            PotionContentsComponent potion = slot.stack().getComponents().getOrDefault(DataComponentTypes.POTION_CONTENTS, null);
            if(potion == null || !potion.hasEffects()) return false;
            PotionContentsComponent otherPotion = slotToCompareTo.stack().getComponents().getOrDefault(DataComponentTypes.POTION_CONTENTS, null);
            if(otherPotion == null || !potion.hasEffects()) return true;
            return (potion.getColor() <= otherPotion.getColor());
        }

        // Blocks comparaison:
        else if(slot.isBlock() && slot.stack().getItem() instanceof BlockItem blockItem && slot.stack().getItem() instanceof BlockItem otherBlockItem) {
            if(skipBlocks) return false;
            if(blockItem.getBlock().getDefaultState().isIn(BlockTags.AXE_MINEABLE) && blockItem.getBlock().getHardness() >= 1.0f) {
                if(otherBlockItem.getBlock().getDefaultState().isIn(BlockTags.AXE_MINEABLE) && otherBlockItem.getBlock().getHardness() >= 1.0f) {
                    return slot.toString().compareTo(slotToCompareTo.toString()) < 1;
                }
                else return true;
            }
            if(otherBlockItem.getBlock().getDefaultState().isIn(BlockTags.AXE_MINEABLE)) return false;
            if(blockItem.getBlock().getDefaultState().isIn(BlockTags.PICKAXE_MINEABLE)) {
                if(otherBlockItem.getBlock().getDefaultState().isIn(BlockTags.PICKAXE_MINEABLE)) {
                    if(ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_CORAL.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_CORAL.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    if(ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS.getOrDefault(blockItem, Integer.MAX_VALUE) < ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS.getOrDefault(otherBlockItem, Integer.MAX_VALUE)) return true;
                    return blockItem.getBlock().getDefaultState().getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color < otherBlockItem.getBlock().getDefaultState().getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color;
                }
                else return true;
            }
            if(otherBlockItem.getBlock().getDefaultState().isIn(BlockTags.PICKAXE_MINEABLE)) return false;
            return blockItem.getBlock().getDefaultState().getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color < otherBlockItem.getBlock().getDefaultState().getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color;
        }

        // Clutch items:
        if(slot.itemType() == ItemType.CLUTCH_TOOL) {
            if(slot.stack().isOf(Items.ENDER_PEARL)) return true;
            if(slot.stack().isOf(Items.WIND_CHARGE)) return true;
            if(slot.stack().isOf(Items.WATER_BUCKET)) return true;
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
            case SHIELD, TOTEM -> EquipmentSlot.OFFHAND;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            case HELMET -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    public static ItemType getItemTypeOf(ItemStack stack) {
        if(stack.isOf(Items.PUFFERFISH) || stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.SPIDER_EYE) || stack.isOf(Items.POISONOUS_POTATO) || stack.isOf(Items.INK_SAC) || stack.isOf(Items.GLOW_INK_SAC)) return ItemType.TRASH;
        if((stack.getComponents().contains(DataComponentTypes.TOOL) || stack.getMaxDamage() > 0) && !stack.getComponents().contains(DataComponentTypes.EQUIPPABLE)) {
            if(stack.isIn(ItemTags.SWORDS)) return ItemType.SWORD;
            else if(stack.isIn(ItemTags.PICKAXES)) return ItemType.PICKAXE;
            else if(stack.isIn(ItemTags.AXES)) return ItemType.AXE;
            else if(stack.isIn(ItemTags.SHOVELS)) return ItemType.SHOVEL;
            else if(stack.isIn(ItemTags.HOES)) return ItemType.HOE;
            else if(stack.isIn(ItemTags.BOW_ENCHANTABLE) && !stack.isOf(Items.CROSSBOW)) return ItemType.BOW;
            else if(stack.isIn(ItemTags.CROSSBOW_ENCHANTABLE) || stack.isOf(Items.CROSSBOW)) return ItemType.CROSSBOW;
            else if(stack.getUseAction() == UseAction.BLOCK || stack.isOf(Items.SHIELD)) return ItemType.SHIELD;
            else if(stack.isOf(Items.SHEARS)) return ItemType.SHEARS;
            else if(stack.isOf(Items.FISHING_ROD)) return ItemType.FISHING_ROD;
            else if(stack.isOf(Items.BRUSH)) return ItemType.MISC_TOOL;
            else if(getAttributeValueWithStack(stack, EquipmentSlot.MAINHAND) > 1.0 || stack.isIn(ItemTags.WEAPON_ENCHANTABLE))
                return ItemType.SPECIAL_WEAPON;
            else return ItemType.MISC_TOOL;
        }
        else if(stack.getComponents().contains(DataComponentTypes.EQUIPPABLE)) {
            EquippableComponent equipComponent = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
            if(!equipComponent.allows(EntityType.PLAYER)) return ItemType.MISC_TOOL;
            else if(stack.isOf(Items.ELYTRA)) return ItemType.ELYTRA;
            else if(equipComponent.slot() == EquipmentSlot.CHEST) return ItemType.CHESTPLATE;
            else if(equipComponent.slot() == EquipmentSlot.LEGS) return ItemType.LEGGINGS;
            else if(equipComponent.slot() == EquipmentSlot.FEET) return ItemType.BOOTS;
            else if(equipComponent.slot() == EquipmentSlot.HEAD) return ItemType.HELMET;
            else return ItemType.MISC_TOOL;
        }
        else if(stack.getComponents().contains(DataComponentTypes.DEATH_PROTECTION)) return ItemType.TOTEM;
        else if(stack.isIn(ItemTags.ARROWS)) return ItemType.ARROWS;
        else if(stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.WIND_CHARGE)) return ItemType.CLUTCH_TOOL;
        else if(stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.COBWEB) || stack.isOf(Items.SNOWBALL) || stack.isOf(Items.FIRE_CHARGE)) return ItemType.COMBAT_ITEMS;
        else if(stack.isOf(Items.SPYGLASS)) return ItemType.SPYGLASS;
        else if(stack.getItem() instanceof SmithingTemplateItem) return ItemType.SMITHING_TEMPLATE;
        else if(stack.getComponents().contains(DataComponentTypes.INSTRUMENT)) return ItemType.MISC_TOOL;
        else if(stack.isOf(Items.MAP) || stack.isOf(Items.FILLED_MAP)) return ItemType.MISC_TOOL;
        else if(stack.isOf(Items.BUCKET) || stack.getRecipeRemainder() == Items.BUCKET.getDefaultStack()) return ItemType.MISC_TOOL;
        else if(MinecraftClient.getInstance().world.getBrewingRecipeRegistry().isPotionRecipeIngredient(stack)) return ItemType.BREWING_INGREDIENT;
        else if(stack.getComponents().contains(DataComponentTypes.FOOD)) return ItemType.FOOD;
        else if(stack.isIn(ItemTags.SHULKER_BOXES)) return ItemType.SHULKER_BOX;
        else if(stack.getComponents().contains(DataComponentTypes.BUNDLE_CONTENTS)) return ItemType.BUNDLE;
        else if(stack.getComponents().contains(DataComponentTypes.CONTAINER)) return ItemType.ITEM_CONTAINER;
        else if(stack.getComponents().contains(DataComponentTypes.CONTAINER_LOOT)) return ItemType.ITEM_CONTAINER;
        else if(stack.getComponents().contains(DataComponentTypes.POTION_CONTENTS)) return ItemType.POTIONS;
        else if(stack.getUseAction() != UseAction.NONE || stack.isOf(Items.TORCH) || stack.isOf(Items.SOUL_TORCH) || stack.isOf(Items.LANTERN) || stack.isOf(Items.SOUL_LANTERN)) return ItemType.TORCHES_AND_LANTERNS;
        else if(stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if(block instanceof CraftingTableBlock || block instanceof BlockWithEntity || block.getDefaultState().isIn(BlockTags.ANVIL)) return ItemType.BLOCK_WORKSTATION;
            if(block.getHardness() > 32F && block.getBlastResistance() > 128F) return ItemType.BLOCK_OTHER;
            if(block.getDefaultState().isOpaqueFullCube()) return ItemType.BLOCK_FULL;
            else if(block.getDefaultState().getLuminance() > 4) return ItemType.MISC_TOOL;
            else if(block instanceof SlabBlock) return ItemType.BLOCK_SLAB;
            else if(block instanceof StairsBlock) return ItemType.BLOCK_STAIRS;
            else if(block instanceof WallBlock) return ItemType.BLOCK_WALL;
            else if(block instanceof FenceBlock) return ItemType.BLOCK_FENCE;
            else if(block instanceof PlantBlock || (block.getHardness() < 1F && block.getDefaultState().getSoundGroup() == BlockSoundGroup.GRASS)) return ItemType.PLANTS_AND_FLOWERS;
            else return ItemType.BLOCK_OTHER;
        }
        else if(stack.getOrDefault(DataComponentTypes.BANNER_PATTERNS, null) != null) return ItemType.BANNER_PATTERNS;
        else if(stack.getItem().getTranslationKey().endsWith("pottery_sherd")) return ItemType.POTTERY;
        else if(stack.isOf(Items.BRICK)) return ItemType.POTTERY;
        else if(stack.isOf(Items.FLOWER_POT)) return ItemType.POTTERY;
        else if(stack.isOf(Items.DISC_FRAGMENT_5)) return ItemType.DISCS;
        else if(stack.getOrDefault(DataComponentTypes.JUKEBOX_PLAYABLE, null) != null) return ItemType.DISCS;
        else return ItemType.MISC;
    }

    private static double getAttributeValueWithStack(ItemStack stack, EquipmentSlot slot) {
        AttributeModifiersComponent attributeModifiersComponent = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        return attributeModifiersComponent.applyOperations(0.0, slot);
    }

    private static double getArmorPreferenceValue(ItemStack stack, EquipmentSlot slot) {
        double attributeValue = getAttributeValueWithStack(stack, slot);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamage())/64.0;
        return attributeValue + durabilityPoints;
    }

    private static double getToolPreferenceValue(ItemStack stack) {
        double attributeValue = getAttributeValueWithStack(stack, EquipmentSlot.MAINHAND);
        double durabilityPoints = (double)(stack.getMaxDamage() * 2 - stack.getDamage())/32.0;
        return attributeValue + durabilityPoints;
    }
}
