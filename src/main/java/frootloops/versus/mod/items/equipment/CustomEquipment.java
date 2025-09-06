package frootloops.versus.mod.items.equipment;

import com.google.common.collect.Maps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.VanillaItemsV2;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static frootloops.versus.mod.items.RegisteringCustomItems.getItemSettings;
import static frootloops.versus.mod.items.RegisteringCustomItems.registerCustomItem;

public abstract class CustomEquipment {

    public static final Item RECOVERY_COMPASS =  new RecoveryCompassItem(getItemSettings("recovery_compass").maxCount(1));

    
    public static final double TRIDENT_SPEED = 1.0, TRIDENT_DAMAGE = 9.0, TRIDENT_REACH = 1.0;
    public static final double PICKAXE_SPEED = 1.2, PICKAXE_DAMAGE = 2.0, PICKAXE_REACH = 0.0;
    public static final double SHOVEL_SPEED = 1.4, SHOVEL_DAMAGE = 3.0, SHOVEL_REACH = 0.0;
    public static final double SWORD_SPEED = 1.6, SWORD_DAMAGE = 3.0, SWORD_REACH = 0.5;
    public static final double HOE_SPEED = 2.0, HOE_DAMAGE = 1.0, HOE_REACH = 1.0;
    public static final double AXE_SPEED = 1.0, AXE_DAMAGE = 6.0, AXE_REACH = 0.0;
    public static final Identifier ATTACK_REACH_MODIFIER_ID = Identifier.of(VersusMod.MOD_ID,"attack_reach_modifier");






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

    public final static HoeItem COPPER_HOE = registerHoe("copper_hoe", COPPER_TOOL_MATERIAL);
    public final static AxeItem COPPER_AXE = registerAxe("copper_axe", COPPER_TOOL_MATERIAL);
    public final static Item COPPER_PICKAXE = registerPickaxe("copper_pickaxe", COPPER_TOOL_MATERIAL);
    public final static Item COPPER_SWORD = registerSword("copper_sword", COPPER_TOOL_MATERIAL, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK);
    public final static ShovelItem COPPER_SHOVEL = new ShovelItem(COPPER_TOOL_MATERIAL, RebalancedTools.getShovelSpeedModifier(), RebalancedTools.getShovelSpeedModifier(), getItemSettings("copper_shovel"));

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

    private static HoeItem registerHoe(String name, ToolMaterial material) {
        HoeItem hoeItem = new HoeItem(material, (float) (HOE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (HOE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED),
                getItemSettings(name).component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(material.attackDamageBonus() + HOE_DAMAGE, HOE_SPEED, HOE_REACH)));
        registerCustomItem(name, hoeItem, ItemGroups.TOOLS);
        return hoeItem;
    }
    private static AxeItem registerAxe(String name, ToolMaterial material) {
        AxeItem axeItem = new AxeItem(material, (float) (AXE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (AXE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED), getItemSettings(name));
        registerCustomItem(name, axeItem, ItemGroups.TOOLS, ItemGroups.COMBAT);
        return axeItem;
    }
    private static Item registerPickaxe(String name, ToolMaterial material) {
        Item pickItem = new Item(getItemSettings(name).pickaxe(material, (float) (PICKAXE_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (PICKAXE_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED)));
        return registerCustomItem(name, pickItem, ItemGroups.TOOLS);
    }

    private static Item registerSword(String name, ToolMaterial material, RegistryEntry.Reference<SoundEvent> soundBlocking, RegistryEntry.Reference<SoundEvent> soundBreaking) {
        Item swordItem = new Item(getItemSettings(name)
                .sword(material, (float) (SWORD_DAMAGE - Combat.PLAYER_BASE_ATTACK_DAMAGE), (float) (SWORD_SPEED - Combat.PLAYER_BASE_ATTACK_SPEED))
                .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(material.attackDamageBonus() + SWORD_DAMAGE, SWORD_SPEED, SWORD_REACH))
                .component(DataComponentTypes.BLOCKS_ATTACKS, getSwordBlockingComponent(material.attackDamageBonus(), soundBlocking, soundBreaking)));
        return registerCustomItem(name, swordItem, ItemGroups.COMBAT);
    }

    public static BlocksAttacksComponent getSwordBlockingComponent(float baseBlockingAmount, RegistryEntry.Reference<SoundEvent> soundBlocking, RegistryEntry.Reference<SoundEvent> soundBreaking) {
        return createDamageBlockingComponent(0.0625F, 0.5F, baseBlockingAmount, 0.5F, soundBlocking, soundBreaking);
    }


    public static BlocksAttacksComponent createDamageBlockingComponent(
            float blockDelaySeconds,    // The amount of time (in seconds) that use must be held before successfully blocking attacks
            float disableCooldownScale, // The multiplier applied to the cooldown time for the item when attacked by a disabling attack
            float amountBlockedBase,    // The constant amount of damage to be blocked
            float amountBlockedFactor,  // The fraction of the dealt damage to be blocked
            RegistryEntry.Reference<SoundEvent> soundBlocking, RegistryEntry.Reference<SoundEvent> soundBreaking
    ) {
        float horizontalBlockingAngle = 90F;
        float itemDamageThreshold = 3.0F;
        float itemDamageBase = 1.0F;
        float itemDamageFactor = 1.0F;
        return new BlocksAttacksComponent(
                blockDelaySeconds,
                disableCooldownScale,
                List.of(new BlocksAttacksComponent.DamageReduction(horizontalBlockingAngle, Optional.empty(), amountBlockedBase, amountBlockedFactor)),
                new BlocksAttacksComponent.ItemDamage(itemDamageThreshold, itemDamageBase, itemDamageFactor),
                Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                Optional.of(SoundEvents.ITEM_SHIELD_BLOCK),
                Optional.of(SoundEvents.ITEM_SHIELD_BREAK)
        );
    }

    public static AttributeModifiersComponent createToolAttributeModifiers(double attackDamage, double attackSpeed, double extraAttackRange) {
        AttributeModifiersComponent.Builder attributeBuilder = AttributeModifiersComponent.builder()
                .add(
                        EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, attackDamage - Combat.PLAYER_BASE_ATTACK_DAMAGE, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND
                )
                .add(
                        EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed - Combat.PLAYER_BASE_ATTACK_SPEED, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND
                );
        if(extraAttackRange != 0.0) {
            attributeBuilder.add(
                    EntityAttributes.ENTITY_INTERACTION_RANGE,
                    new EntityAttributeModifier(ATTACK_REACH_MODIFIER_ID, extraAttackRange, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND
            );
        }
        return attributeBuilder.build();
    }
}