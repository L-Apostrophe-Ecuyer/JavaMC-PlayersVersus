package frootloops.versus.mod.items.equipment;

import com.google.common.collect.Maps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Map;

import static frootloops.versus.mod.Combat.*;

import static frootloops.versus.mod.items.RegisteringCustomItems.getItemSettings;
import static frootloops.versus.mod.items.VanillaItemsV2.createToolAttributeModifiers;
import static frootloops.versus.mod.items.VanillaItemsV2.getSwordBlockingComponent;

public abstract class CustomEquipment {

    public static final Item RECOVERY_COMPASS =  new RecoveryCompassItem(getItemSettings("recovery_compass").maxCount(1));

    static RegistryKey<? extends Registry<EquipmentAsset>> ARMOR_ASSET_REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.of(VersusMod.MOD_ID, "equipment_asset"));
    public static final int COPPER_ENCHANTABILITY = 3, COPPER_ARMOR_DURABILITY = 4;
    public static final TagKey<Item> COPPER_TOOL_MATERIALS_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, "copper_tool_materials"));
    public static final ToolMaterial COPPER_TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 128, 13.0F, 1.0F, COPPER_ENCHANTABILITY, COPPER_TOOL_MATERIALS_TAG);
    static final ArmorMaterial COPPER_ARMOR_MATERIAL = new ArmorMaterial(
            COPPER_ARMOR_DURABILITY, createDefenseMap(2, 3, 5, 2, 7), COPPER_ENCHANTABILITY, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.0F, 0.0F, COPPER_TOOL_MATERIALS_TAG,
            getArmorAsset("copper")
    );

    static final ArmorMaterial COPPER_ARMOR_EXPOSED_MATERIAL = new ArmorMaterial(
            COPPER_ARMOR_DURABILITY, createDefenseMap(1, 2, 4, 1, 6), COPPER_ENCHANTABILITY - 1, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.0F, 0.0F, COPPER_TOOL_MATERIALS_TAG,
            getArmorAsset("copper_damaged")
    );


    public static final Item COPPER_HELMET_DAMAGED = new DecayableArmorItem(COPPER_ARMOR_EXPOSED_MATERIAL, EquipmentType.HELMET, getItemSettings("copper_helmet_damaged"), Items.GOLDEN_HELMET, 64);
    public static final Item COPPER_CHESTPLATE_DAMAGED = new DecayableArmorItem(COPPER_ARMOR_EXPOSED_MATERIAL, EquipmentType.CHESTPLATE, getItemSettings("copper_chestplate_damaged"), Items.GOLDEN_CHESTPLATE, 80);
    public static final Item COPPER_LEGGINGS_DAMAGED = new DecayableArmorItem(COPPER_ARMOR_EXPOSED_MATERIAL, EquipmentType.LEGGINGS, getItemSettings("copper_leggings_damaged"), Items.GOLDEN_LEGGINGS, 72);
    public static final Item COPPER_BOOTS_DAMAGED = new DecayableArmorItem(COPPER_ARMOR_EXPOSED_MATERIAL, EquipmentType.BOOTS, getItemSettings("copper_boots_damaged"), Items.GOLDEN_BOOTS, 64);
    public static final Item COPPER_HELMET = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.HELMET, getItemSettings("copper_helmet"), COPPER_HELMET_DAMAGED, 24);
    public static final Item COPPER_CHESTPLATE = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.CHESTPLATE, getItemSettings("copper_chestplate"), COPPER_CHESTPLATE_DAMAGED, 36);
    public static final Item COPPER_LEGGINGS = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.LEGGINGS, getItemSettings("copper_leggings"), COPPER_LEGGINGS_DAMAGED, 32);
    public static final Item COPPER_BOOTS = new DecayableArmorItem(COPPER_ARMOR_MATERIAL, EquipmentType.BOOTS, getItemSettings("copper_boots"), COPPER_BOOTS_DAMAGED, 24);

    public final static HoeItem COPPER_HOE = customHoe("copper_hoe", COPPER_TOOL_MATERIAL);
    public final static AxeItem COPPER_AXE = customAxe("copper_axe", COPPER_TOOL_MATERIAL);
    public final static Item COPPER_PICKAXE = customPickaxe("copper_pickaxe", COPPER_TOOL_MATERIAL);
    public final static Item COPPER_SWORD = customSword("copper_sword", COPPER_TOOL_MATERIAL, CustomSpecialEffects.SWORD_BLOCKING_METAL);
    public final static ShovelItem COPPER_SHOVEL = customShovel("copper_shovel", COPPER_TOOL_MATERIAL);

    private static RegistryKey<EquipmentAsset> getArmorAsset(String id) {
        return RegistryKey.of(ARMOR_ASSET_REGISTRY_KEY, Identifier.of(VersusMod.MOD_ID, id));
    }

    private static Map<EquipmentType, Integer> createDefenseMap(int bootsDefense, int leggingsDefense, int chestplateDefense, int helmetDefense, int bodyDefense) {
        return Maps.newEnumMap(
                Map.of(
                        EquipmentType.BOOTS, bootsDefense,
                        EquipmentType.LEGGINGS, leggingsDefense,
                        EquipmentType.CHESTPLATE, chestplateDefense,
                        EquipmentType.HELMET, helmetDefense,
                        EquipmentType.BODY, bodyDefense
                )
        );
    }

    private static ShovelItem customShovel(String name, ToolMaterial material) {
        return new ShovelItem(material, (float) (SHOVEL_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (SHOVEL_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED),
                getItemSettings(name).component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(material.attackDamageBonus() + SHOVEL_DAMAGE, SHOVEL_SPEED, SHOVEL_REACH)));
    }
    private static HoeItem customHoe(String name, ToolMaterial material) {
        return new HoeItem(material, (float) (HOE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (HOE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED),
                getItemSettings(name).component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(material.attackDamageBonus() + HOE_DAMAGE, HOE_SPEED, HOE_REACH)));
    }
    private static AxeItem customAxe(String name, ToolMaterial material) {
        return new AxeItem(material, (float) (AXE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (AXE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED), getItemSettings(name));
    }
    private static Item customPickaxe(String name, ToolMaterial material) {
        return new Item(getItemSettings(name).pickaxe(material, (float) (PICKAXE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (PICKAXE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED)));
    }

    private static Item customSword(String name, ToolMaterial material, RegistryEntry.Reference<SoundEvent> soundBlocking) {
        return new Item(getItemSettings(name)
                .sword(material, (float) (SWORD_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (SWORD_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED))
                .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(material.attackDamageBonus() + SWORD_DAMAGE, SWORD_SPEED, SWORD_REACH))
                .component(DataComponentTypes.BLOCKS_ATTACKS, getSwordBlockingComponent(material.attackDamageBonus(), soundBlocking, SoundEvents.ITEM_SHIELD_BREAK)));
    }
}