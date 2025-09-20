package frootloops.versus.mod.items_and_effects.equipment;

import frootloops.versus.VersusMod;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getSettings;

public abstract class CustomEquipment {

    public static final Item RECOVERY_COMPASS =  new RecoveryCompassItem(getSettings("recovery_compass").maxCount(1));
    public static final int COPPER_ENCHANTABILITY = 1;
    public static final TagKey<Item> COPPER_TOOL_MATERIALS_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, "copper_tool_materials"));
    public static final ToolMaterial COPPER_TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 128, 13.0F, 1.0F, COPPER_ENCHANTABILITY, COPPER_TOOL_MATERIALS_TAG);
    public static final Identifier COPPER_EQUIPMENT_MODEL_ID =  Identifier.of(VersusMod.MOD_ID, "copper");
    public static final Identifier COPPER_EQUIPMENT_DAMAGED_MODEL_ID =  Identifier.of(VersusMod.MOD_ID, "copper_damaged");


    static ArmorMaterial COPPER_ARMOR_MATERIAL = new ArmorMaterial(4, Util.make(new EnumMap(EquipmentType.class), map -> {
        map.put(EquipmentType.BOOTS, 1);
        map.put(EquipmentType.LEGGINGS, 3);
        map.put(EquipmentType.CHESTPLATE, 5);
        map.put(EquipmentType.HELMET, 2);
        map.put(EquipmentType.BODY, 7);
    }), COPPER_ENCHANTABILITY, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, COPPER_TOOL_MATERIALS_TAG, COPPER_EQUIPMENT_MODEL_ID);

    static ArmorMaterial COPPER_ARMOR_DAMAGED_MATERIAL = new ArmorMaterial(4, Util.make(new EnumMap(EquipmentType.class), map -> {
        map.put(EquipmentType.BOOTS, 1);
        map.put(EquipmentType.LEGGINGS, 2);
        map.put(EquipmentType.CHESTPLATE, 3);
        map.put(EquipmentType.HELMET, 1);
        map.put(EquipmentType.BODY, 5);
    }), COPPER_ENCHANTABILITY, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, COPPER_TOOL_MATERIALS_TAG, COPPER_EQUIPMENT_DAMAGED_MODEL_ID);

    public static final ArmorItem COPPER_HELMET_DAMAGED = new ArmorItem(COPPER_ARMOR_DAMAGED_MATERIAL, EquipmentType.HELMET, getSettings("copper_helmet_damaged"));
    public static final ArmorItem COPPER_CHESTPLATE_DAMAGED = new ArmorItem(COPPER_ARMOR_DAMAGED_MATERIAL, EquipmentType.CHESTPLATE, getSettings("copper_chestplate_damaged"));
    public static final ArmorItem COPPER_LEGGINGS_DAMAGED = new ArmorItem(COPPER_ARMOR_DAMAGED_MATERIAL, EquipmentType.LEGGINGS, getSettings("copper_leggings_damaged"));
    public static final ArmorItem COPPER_BOOTS_DAMAGED = new ArmorItem(COPPER_ARMOR_DAMAGED_MATERIAL, EquipmentType.BOOTS, getSettings("copper_boots_damaged"));
    public static final ArmorItem COPPER_HELMET = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.HELMET, getSettings("copper_helmet"), COPPER_HELMET_DAMAGED);
    public static final ArmorItem COPPER_CHESTPLATE = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.CHESTPLATE, getSettings("copper_chestplate"), COPPER_CHESTPLATE_DAMAGED);
    public static final ArmorItem COPPER_LEGGINGS = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.LEGGINGS, getSettings("copper_leggings"), COPPER_LEGGINGS_DAMAGED);
    public static final ArmorItem COPPER_BOOTS = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.BOOTS, getSettings("copper_boots"), COPPER_BOOTS_DAMAGED);

    public final static HoeItem COPPER_HOE = new HoeItem(COPPER_TOOL_MATERIAL, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), getSettings("copper_hoe").attributeModifiers(RebalancedTools.getHoeReachModifier()));
    public final static AxeItem COPPER_AXE = new AxeItem(COPPER_TOOL_MATERIAL, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), getSettings("copper_axe"));
    public final static PickaxeItem COPPER_PICKAXE = new PickaxeItem(COPPER_TOOL_MATERIAL, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), getSettings("copper_pickaxe"));
    public final static SwordItem COPPER_SWORD = new SwordItem(COPPER_TOOL_MATERIAL, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), getSettings("copper_sword").attributeModifiers(RebalancedTools.getSwordReachModifier()));
    public final static ShovelItem COPPER_SHOVEL = new ShovelItem(COPPER_TOOL_MATERIAL, RebalancedTools.getShovelSpeedModifier(), RebalancedTools.getShovelSpeedModifier(), getSettings("copper_shovel"));

}