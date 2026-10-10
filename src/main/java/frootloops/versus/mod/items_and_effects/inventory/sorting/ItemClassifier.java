package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.mod.items_and_effects.brewing.BrewingSystem;
import frootloops.versus.mod.items_and_effects.brewing.ConcentrateItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
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

/**
 * The {@link ItemType} a stack sorts as.
 */
public final class ItemClassifier {

    private ItemClassifier() {
    }

    public static ItemType typeOf(ItemStack stack) {
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
            else if(attributeTotal(stack, EquipmentSlot.MAINHAND) > 1.0 || stack.is(ItemTags.WEAPON_ENCHANTABLE))
                return ItemType.SPECIAL_WEAPON;
            else return ItemType.MISC_TOOL;
        }
        else if(stack.getComponents().has(DataComponents.EQUIPPABLE)) {
            Equippable equipComponent = stack.getComponents().get(DataComponents.EQUIPPABLE);
            if(!equipComponent.canBeEquippedBy(EntityTypes.PLAYER.builtInRegistryHolder())) return ItemType.MISC_TOOL;
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
        else if(stack.is(Items.BUCKET) || leavesBucket(stack)) return ItemType.MISC_TOOL;
        else if(BrewingSystem.isIngredient(stack.getItem())  || stack.getItem() instanceof ConcentrateItem) return ItemType.BREWING_INGREDIENT;
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

    static double attributeTotal(ItemStack stack, EquipmentSlot slot) {
        // What the item's modifiers add up to in this slot, whichever attributes they belong to (1.21.10's compute, which
        // 26.3 limits to one attribute): the sorting ranks items by how strong they are, attack damage or armor alike.
        ItemAttributeModifiers attributeModifiersComponent = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double total = 0.0;
        for (ItemAttributeModifiers.Entry entry : attributeModifiersComponent.modifiers()) {
            if (!entry.slot().test(slot)) continue;
            double amount = entry.modifier().amount();
            total += switch (entry.modifier().operation()) {
                case ADD_VALUE -> amount;
                case ADD_MULTIPLIED_BASE -> 0.0;
                case ADD_MULTIPLIED_TOTAL -> amount * total;
            };
        }
        return total;
    }

    /** Items that leave a bucket behind when crafted with (milk, water and so on), which sort with the buckets. */
    private static boolean leavesBucket(ItemStack stack) {
        ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder != null && remainder.is(Items.BUCKET);
    }
}
