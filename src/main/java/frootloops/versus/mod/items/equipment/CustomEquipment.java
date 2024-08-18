package frootloops.versus.mod.items.equipment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.equipment.custom.copper.CopperToolMaterial;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class CustomEquipment {

    public static final TagKey<Item> COPPER_TOOL_MATERIALS_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, "copper_tool_materials"));
    public static final ToolMaterial COPPER_TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 128, 13.0F, 1.0F, 1, COPPER_TOOL_MATERIALS_TAG);
    public static final RegistryEntry<ArmorMaterial> COPPER_ARMOR_MATERIAL = registerCustomArmorMaterial("copper", Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 1);
        map.put(ArmorItem.Type.LEGGINGS, 3);
        map.put(ArmorItem.Type.CHESTPLATE, 5);
        map.put(ArmorItem.Type.HELMET, 2);
        map.put(ArmorItem.Type.BODY, 7);
    }), 1, SoundEvents.ITEM_ARMOR_EQUIP_TURTLE, 0.0f, 0.0f, () -> Ingredient.ofItems(net.minecraft.item.Items.COPPER_INGOT));

    public static final Item COPPER_HELMET = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(10)));
    public static final Item COPPER_CHESTPLATE = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(10)));
    public static final Item COPPER_LEGGINGS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(10)));
    public static final Item COPPER_BOOTS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(10)));
    public static HoeItem COPPER_HOE = new HoeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier())));
    public static AxeItem COPPER_AXE = new AxeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier())));
    public static PickaxeItem COPPER_PICKAXE = new PickaxeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier())));
    public static SwordItem COPPER_SWORD = new SwordItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier())));
    public static ShovelItem COPPER_SHOVEL = new ShovelItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier())));


    public static RegistryEntry<ArmorMaterial> registerCustomArmorMaterial(String name, EnumMap<ArmorItem.Type, Integer> defense, int enchantability, RegistryEntry<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        Identifier id = Identifier.of(MOD_ID, name);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(id));
        EnumMap<ArmorItem.Type, Integer> enumMap = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            enumMap.put(type, defense.get(type));
        }
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id, new ArmorMaterial(enumMap, enchantability, equipSound, repairIngredient, layers, toughness, knockbackResistance));
    }
}