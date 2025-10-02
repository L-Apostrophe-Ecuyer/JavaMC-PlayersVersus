package frootloops.versus.mod.items_and_effects;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static frootloops.versus.mod.Combat.*;

public class VanillaItems {
    private static Map<Item, Item> ITEM_REPLACEMENT_MAP = new HashMap<>();

    public static void onInitialize() {
        setUpTransformVanillaItemsToModded(); // This is to replace vanilla items with modded ones, when components don't do the job

        // Modify default components:
        DefaultItemComponentEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.of(VersusMod.MOD_ID, "late"));

        // Modify tools:
        modifyVanillaToolsAndWeapons();

        // Modify Stack Sizes:
        modifyVanillaStackSizes();

        // Modify foods:
        modifyVanillaFoods();

        // Other tweaks:
        DefaultItemComponentEvents.MODIFY.register(context -> {

            // Add food component to glistering melon slices:
            context.modify(Items.GLISTERING_MELON_SLICE, builder -> {builder
                    .add(DataComponentTypes.FOOD, new FoodComponent.Builder().nutrition(4).saturationModifier(1.0F).build())
                    .add(DataComponentTypes.CONSUMABLE, createFoodConsumptionComponent(1.4F, true, new ApplyEffectsConsumeEffect(
                            List.of(new StatusEffectInstance(StatusEffects.REGENERATION, 10, 2))
                    )));
            });

            // Give leather armor some knockback resistance:
            context.modify(Items.LEATHER_CHESTPLATE, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.CHESTPLATE, 2.0, 0.0, 0.1));});
            context.modify(Items.LEATHER_LEGGINGS, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.LEGGINGS, 2.0, 0.0, 0.05));});
            context.modify(Items.LEATHER_BOOTS, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.BOOTS, 1.0, 0.0, 0.0, -0.2));});

            // Give chainmail armor some toughness:
            context.modify(Items.CHAINMAIL_CHESTPLATE, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.CHESTPLATE, 5.0, 3.0, 0.0));});
            context.modify(Items.CHAINMAIL_LEGGINGS, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.LEGGINGS, 4.0, 2.0, 0.0));});
            context.modify(Items.CHAINMAIL_BOOTS, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.BOOTS, 2.0, 2.0, 0.0));});
            context.modify(Items.CHAINMAIL_HELMET, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createArmorAttributes(EquipmentType.HELMET, 2.0, 2.0, 0.0));});

            // Give turtle helmets more buffs:
            context.modify(Items.TURTLE_HELMET, builder -> {builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createTurtleArmorAttributes(EquipmentType.HELMET, 2.0));});
        });
    }

    private static void setUpTransformVanillaItemsToModded() {

        ITEM_REPLACEMENT_MAP.put(Items.COPPER_AXE, CustomEquipment.COPPER_AXE);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_HOE, CustomEquipment.COPPER_HOE);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_SHOVEL, CustomEquipment.COPPER_SHOVEL);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_SWORD, CustomEquipment.COPPER_SWORD);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_PICKAXE, CustomEquipment.COPPER_PICKAXE);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_HELMET, CustomEquipment.COPPER_HELMET);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_BOOTS, CustomEquipment.COPPER_BOOTS);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_LEGGINGS, CustomEquipment.COPPER_LEGGINGS);
        ITEM_REPLACEMENT_MAP.put(Items.COPPER_CHESTPLATE, CustomEquipment.COPPER_CHESTPLATE);


        ITEM_REPLACEMENT_MAP.put(Items.FERMENTED_SPIDER_EYE, CustomBrewingItems.CORRUPTED_WART_POWDER);
        ITEM_REPLACEMENT_MAP.put(Items.BLAZE_POWDER, CustomBrewingItems.CONCENTRATE_OF_STRENGTH);
        ITEM_REPLACEMENT_MAP.put(Items.MAGMA_CREAM, CustomBrewingItems.CONCENTRATE_OF_FIRE);
        ITEM_REPLACEMENT_MAP.put(Items.RECOVERY_COMPASS, CustomEquipment.RECOVERY_COMPASS);

        ITEM_REPLACEMENT_MAP.put(Items.LADDER, CustomBlockItems.LADDER);
        ITEM_REPLACEMENT_MAP.put(Items.CLAY, CustomBlockItems.GRAY_CLAY);
        ITEM_REPLACEMENT_MAP.put(Items.MUD, CustomBlockItems.GRAY_MUD);

        ITEM_REPLACEMENT_MAP.put(Items.PACKED_MUD, CustomBlockItems.BROWN_CLAY);
        ITEM_REPLACEMENT_MAP.put(Items.MUD_BRICKS, CustomBlockItems.BROWN_CLAY_BRICKS);
        ITEM_REPLACEMENT_MAP.put(Items.MUD_BRICK_SLAB, CustomBlockItems.BROWN_CLAY_BRICK_SLAB);
        ITEM_REPLACEMENT_MAP.put(Items.MUD_BRICK_STAIRS, CustomBlockItems.BROWN_CLAY_BRICK_STAIRS);
        ITEM_REPLACEMENT_MAP.put(Items.MUD_BRICK_WALL, CustomBlockItems.BROWN_MUD_BRICK_WALL);
    }

    public static Item getReplacementItem(Item item) {
        return ITEM_REPLACEMENT_MAP.getOrDefault(item, item);
    }

    public static boolean hasReplacementItem(Item item) {
        if(item == Items.PACKED_MUD) VersusMod.MOD_LOGGER.warn(" -> Is Packed Mud in map? " + ITEM_REPLACEMENT_MAP.containsKey(item));
        return ITEM_REPLACEMENT_MAP.containsKey(item);
    }

    private static void modifyVanillaStackSizes() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            // Throwables:
            modifyVanillaStackSizeOf(context, Items.EGG, VersusSettings.Items.MAX_COUNT_THROWABLE);
            modifyVanillaStackSizeOf(context, Items.SNOWBALL, VersusSettings.Items.MAX_COUNT_THROWABLE);
            modifyVanillaStackSizeOf(context, Items.ENDER_PEARL, VersusSettings.Items.MAX_COUNT_THROWABLE);
            modifyVanillaStackSizeOf(context, Items.FIRE_CHARGE, VersusSettings.Items.MAX_COUNT_THROWABLE);

            // Minecarts:
            modifyVanillaStackSizeOf(context, Items.MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.TNT_MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.CHEST_MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.HOPPER_MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.FURNACE_MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.COMMAND_BLOCK_MINECART, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);

            // Boats:
            modifyVanillaStackSizeOf(context, Items.OAK_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.BIRCH_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.ACACIA_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.CHERRY_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.JUNGLE_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.SPRUCE_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.BAMBOO_RAFT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.DARK_OAK_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.PALE_OAK_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.MANGROVE_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.OAK_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.BIRCH_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.ACACIA_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.CHERRY_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.JUNGLE_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.SPRUCE_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.BAMBOO_CHEST_RAFT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.DARK_OAK_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.PALE_OAK_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
            modifyVanillaStackSizeOf(context, Items.MANGROVE_CHEST_BOAT, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);

            // Others:
            modifyVanillaStackSizeOf(context, Items.POTION, VersusSettings.Items.MAX_COUNT_BOTTLED);
            modifyVanillaStackSizeOf(context, Items.POWDER_SNOW_BUCKET, VersusSettings.Items.MAX_COUNT_BUCKETS);
            modifyVanillaStackSizeOf(context, Items.WATER_BUCKET, VersusSettings.Items.MAX_COUNT_BUCKETS);
            modifyVanillaStackSizeOf(context, Items.MILK_BUCKET, VersusSettings.Items.MAX_COUNT_BUCKETS);
            modifyVanillaStackSizeOf(context, Items.CAKE, VersusSettings.Items.MAX_COUNT_FOOD);
            modifyVanillaStackSizeOf(context, Items.RECOVERY_COMPASS, 1);

            // Misc
            for (Item item : Registries.ITEM) {
                if(item.getMaxCount() == 1) {
                    if (item.getDefaultStack().getComponents().contains(DataComponentTypes.MAX_DAMAGE)) return;
                    EquippableComponent equipComponent = item.getDefaultStack().getComponents().getOrDefault(DataComponentTypes.EQUIPPABLE, null);
                    if (equipComponent != null && (equipComponent.slot() == EquipmentSlot.SADDLE || equipComponent.slot() == EquipmentSlot.BODY))
                        modifyVanillaStackSizeOf(context, item, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
                    else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                        modifyVanillaStackSizeOf(context, item, VersusSettings.Items.MAX_COUNT_PLACEABLE_ENTITIES);
                }
            }
        });
    }

    private static void modifyVanillaFoods() {
        modifyVanillaFoodItem(Items.APPLE, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.BAKED_POTATO, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.BEEF, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.BEETROOT, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.BEETROOT_SOUP, VersusSettings.Items.EAT_TIME_LIQUIDS);
        modifyVanillaFoodItem(Items.BREAD, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.CARROT, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.CHICKEN, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.CHORUS_FRUIT, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.COD, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_BEEF, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_CHICKEN, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_COD, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_MUTTON, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_PORKCHOP, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_RABBIT, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKED_SALMON, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.COOKIE, VersusSettings.Items.EAT_TIME_SNACK);
        modifyVanillaFoodItem(Items.DRIED_KELP, VersusSettings.Items.EAT_TIME_SNACK);
        modifyVanillaFoodItem(Items.ENCHANTED_GOLDEN_APPLE, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.GOLDEN_APPLE, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.GOLDEN_CARROT, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.HONEY_BOTTLE, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.MELON_SLICE, VersusSettings.Items.EAT_TIME_SNACK);
        modifyVanillaFoodItem(Items.MUSHROOM_STEW, VersusSettings.Items.EAT_TIME_LIQUIDS);
        modifyVanillaFoodItem(Items.MUTTON, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.POISONOUS_POTATO, VersusSettings.Items.EAT_TIME_POISON);
        modifyVanillaFoodItem(Items.PORKCHOP, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.POTATO, VersusSettings.Items.EAT_TIME_REGULAR);
        modifyVanillaFoodItem(Items.PUFFERFISH, VersusSettings.Items.EAT_TIME_POISON);
        modifyVanillaFoodItem(Items.PUMPKIN_PIE, VersusSettings.Items.EAT_TIME_SNACK);
        modifyVanillaFoodItem(Items.RABBIT, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.RABBIT_STEW, VersusSettings.Items.EAT_TIME_LIQUIDS);
        modifyVanillaFoodItem(Items.ROTTEN_FLESH, VersusSettings.Items.EAT_TIME_POISON);
        modifyVanillaFoodItem(Items.SALMON, VersusSettings.Items.EAT_TIME_MEAT);
        modifyVanillaFoodItem(Items.SPIDER_EYE, VersusSettings.Items.EAT_TIME_POISON);
        modifyVanillaFoodItem(Items.SUSPICIOUS_STEW, VersusSettings.Items.EAT_TIME_LIQUIDS);
        modifyVanillaFoodItem(Items.SWEET_BERRIES, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.GLOW_BERRIES, VersusSettings.Items.EAT_TIME_VEGGIES);
        modifyVanillaFoodItem(Items.TROPICAL_FISH, VersusSettings.Items.EAT_TIME_SNACK);
        modifyVanillaFoodItem(Items.MILK_BUCKET, VersusSettings.Items.EAT_TIME_LIQUIDS);
    }

    private static void modifyVanillaStackSizeOf(DefaultItemComponentEvents.ModifyContext context, Item item, int newStackSize) {
        if(item.getDefaultStack().getMaxCount() == newStackSize) return;
        context.modify(item, builder -> {builder.add(DataComponentTypes.MAX_STACK_SIZE, newStackSize + 0);});
    }

    private static void modifyVanillaFoodItem(final Item item, final float eatTime) {
        UseRemainderComponent remainderComponent = item.getDefaultStack().getOrDefault(DataComponentTypes.USE_REMAINDER, null);
        boolean isHoneyBottle = item == Items.HONEY_BOTTLE;
        boolean isBottled = !isHoneyBottle && (remainderComponent != null && remainderComponent.convertInto().isOf(Items.GLASS_BOTTLE));
        boolean isBucket = !isBottled && remainderComponent != null && remainderComponent.convertInto().isOf(Items.BUCKET);
        boolean isStew = !isBucket && !isBottled && remainderComponent != null && remainderComponent.convertInto().isOf(Items.BOWL);

        ConsumableComponent consumeComponent = item.getDefaultStack().getOrDefault(DataComponentTypes.CONSUMABLE, null);
        boolean isDrink = isHoneyBottle || isStew || isBucket || isBottled || consumeComponent.sound() == SoundEvents.ENTITY_GENERIC_DRINK;
        boolean hasParticles = !isDrink && remainderComponent == null;

        ConsumableComponent newConsumeComponent = isDrink ?
                createDrinkConsumptionComponent(eatTime, consumeComponent.onConsumeEffects(), isHoneyBottle) :
                createFoodConsumptionComponent(eatTime, hasParticles, consumeComponent.onConsumeEffects());

        final int maxCount;
        if(isBottled) maxCount = VersusSettings.Items.MAX_COUNT_BOTTLED;
        else if(isStew) maxCount = VersusSettings.Items.MAX_COUNT_STEWS;
        else if(isBucket) maxCount = VersusSettings.Items.MAX_COUNT_BUCKETS;
        else maxCount = VersusSettings.Items.MAX_COUNT_FOOD;
        DefaultItemComponentEvents.MODIFY.register(context -> {
            if(maxCount != item.getDefaultStack().getMaxCount()) modifyVanillaStackSizeOf(context, item, maxCount);
            context.modify(item, builder -> {builder.add(DataComponentTypes.CONSUMABLE, newConsumeComponent);});
        });
    }


    private static void modifyVanillaToolsAndWeapons() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            modifyToolComponents(context, Items.TRIDENT, TRIDENT_DAMAGE, TRIDENT_SPEED, TRIDENT_REACH);

            // Shields are instant:
            context.modify(Items.SHIELD, builder -> {
                builder.add(DataComponentTypes.BLOCKS_ATTACKS, createDamageBlockingComponent(0.0625F, 1.0F, 0.0F, 1.0F, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK));
            });

            double extra = 0.0;
            modifySwordComponents(context, Items.WOODEN_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 0F, CustomSpecialEffects.SWORD_BLOCKING_WOOD);
            modifyToolComponents(context, Items.WOODEN_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH, 1.0);
            modifyToolComponents(context, Items.WOODEN_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.WOODEN_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.WOODEN_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

            extra = 1.0;
            modifySwordComponents(context, Items.STONE_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 0F, CustomSpecialEffects.SWORD_BLOCKING_STONE);
            modifyToolComponents(context, Items.STONE_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
            modifyToolComponents(context, Items.STONE_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.STONE_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.STONE_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

            extra = 2.0;
            modifySwordComponents(context, Items.IRON_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 1F, CustomSpecialEffects.SWORD_BLOCKING_METAL);
            modifyToolComponents(context, Items.IRON_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH, 1.0);
            modifyToolComponents(context, Items.IRON_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.IRON_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.IRON_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

            modifySwordComponents(context, Items.GOLDEN_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 1F, CustomSpecialEffects.SWORD_BLOCKING_METAL);
            modifyToolComponents(context, Items.GOLDEN_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH, 1.0);
            modifyToolComponents(context, Items.GOLDEN_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.GOLDEN_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.GOLDEN_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

            extra = 3.0;
            modifySwordComponents(context, Items.DIAMOND_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 2F, CustomSpecialEffects.SWORD_BLOCKING_DIAMOND);
            modifyToolComponents(context, Items.DIAMOND_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH, 1.0);
            modifyToolComponents(context, Items.DIAMOND_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.DIAMOND_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.DIAMOND_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

            extra = 4.0;
            modifySwordComponents(context, Items.NETHERITE_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, 3F, CustomSpecialEffects.SWORD_BLOCKING_METAL);
            modifyToolComponents(context, Items.NETHERITE_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH, 1.0);
            modifyToolComponents(context, Items.NETHERITE_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
            modifyToolComponents(context, Items.NETHERITE_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
            modifyToolComponents(context, Items.NETHERITE_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);
        });
    }

    private static void modifySwordComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, float baseBlockingAmount, RegistryEntry.Reference<SoundEvent> blockingSound) {
        context.modify(item, builder -> {builder
                .add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange, 0.0))
                .add(DataComponentTypes.BLOCKS_ATTACKS, getSwordBlockingComponent(baseBlockingAmount, blockingSound, blockingSound));
        });
    }

    private static void modifyToolComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange) {
        modifyToolComponents(context, item, attackDamage, attackSpeed, extraAttackRange, 0.0);
    }

    private static void modifyToolComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, double extraAttackKnockback) {
        context.modify(item, builder -> {
            builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange, extraAttackKnockback));
        });
    }

    public static ConsumableComponent createDrinkConsumptionComponent(float consumeSeconds, List<ConsumeEffect> consumeEffects, boolean usHoneySound) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(consumeSeconds).useAction(UseAction.DRINK).sound(usHoneySound ? SoundEvents.ITEM_HONEY_BOTTLE_DRINK : SoundEvents.ENTITY_GENERIC_DRINK).consumeParticles(false);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.consumeEffect(effect);
        return consumeComponent.build();
    }

    public static ConsumableComponent createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, ApplyEffectsConsumeEffect applyEffectsConsumeEffect) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(consumeSeconds).useAction(UseAction.EAT).sound(SoundEvents.ENTITY_GENERIC_EAT).consumeParticles(doParticles);
        if(applyEffectsConsumeEffect != null) consumeComponent.consumeEffect(applyEffectsConsumeEffect);
        return consumeComponent.build();
    }
    public static ConsumableComponent createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, List<ConsumeEffect> consumeEffects) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(consumeSeconds).useAction(UseAction.EAT).sound(SoundEvents.ENTITY_GENERIC_EAT).consumeParticles(doParticles);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.consumeEffect(effect);
        return consumeComponent.build();
    }

    public static ConsumableComponent createUseActionComponent(UseAction useAction, float consumeSeconds, RegistryEntry<SoundEvent> sound, RegistryEntry<SoundEvent> finishSound, boolean consumeParticles, ConsumeEffect consumeEffect) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().useAction(useAction).consumeSeconds(consumeSeconds).sound(sound).finishSound(finishSound).consumeParticles(consumeParticles).consumeEffect(consumeEffect);
        return consumeComponent.build();
    }

    public static BlocksAttacksComponent getSwordBlockingComponent(float baseBlockingAmount, RegistryEntry.Reference<SoundEvent> soundBlocking, RegistryEntry.Reference<SoundEvent> soundBreaking) {
        return createDamageBlockingComponent(0.0625F, 0.5F, Math.max(0F, baseBlockingAmount), 0.5F, soundBlocking, soundBreaking);
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
                Optional.of(soundBlocking),
                Optional.of(soundBreaking)
        );
    }

    public static AttributeModifiersComponent createToolAttributeModifiers(double attackDamage, double attackSpeed, double extraAttackRange, double extraAttackKnockback) {
        AttributeModifiersComponent.Builder attributeBuilder = AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, attackDamage - Combat.PLAYER_BASE_ATTACK_DAMAGE, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed - Combat.PLAYER_BASE_ATTACK_SPEED, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);

        if(extraAttackRange != 0.0)
            attributeBuilder.add(EntityAttributes.ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(ATTACK_REACH_MODIFIER_ID, extraAttackRange, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);

        if(extraAttackKnockback != 0.0)
            attributeBuilder.add(EntityAttributes.ATTACK_KNOCKBACK, new EntityAttributeModifier(ATTACK_KNOCKBACK_MODIFIER_ID, extraAttackRange, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);

        return attributeBuilder.build();
    }


    public static AttributeModifiersComponent createArmorAttributes(EquipmentType type, double armor, double toughness, double kbResistance) {
        return createArmorAttributes(type, armor, toughness, kbResistance, 0.0);
    }
    public static AttributeModifiersComponent createArmorAttributes(EquipmentType type, double armor, double toughness, double kbResistance, double fallDmgMultiplier) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        AttributeModifierSlot attributeModifierSlot = AttributeModifierSlot.forEquipmentSlot(type.getEquipmentSlot());
        Identifier identifier = Identifier.ofVanilla("armor." + type.getName());

        builder.add(EntityAttributes.ARMOR, new EntityAttributeModifier(identifier, armor, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (toughness != 0.0) builder.add(EntityAttributes.ARMOR_TOUGHNESS, new EntityAttributeModifier(identifier, toughness, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (kbResistance > 0.0) builder.add(EntityAttributes.KNOCKBACK_RESISTANCE, new EntityAttributeModifier(identifier, kbResistance, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (fallDmgMultiplier != 0.0) builder.add(EntityAttributes.FALL_DAMAGE_MULTIPLIER, new EntityAttributeModifier(identifier, fallDmgMultiplier, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);

        return builder.build();
    }

    private static AttributeModifiersComponent createTurtleArmorAttributes(EquipmentType type, double armor) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        AttributeModifierSlot attributeModifierSlot = AttributeModifierSlot.forEquipmentSlot(type.getEquipmentSlot());
        Identifier identifier = Identifier.ofVanilla("armor." + type.getName());

        builder.add(EntityAttributes.ARMOR, new EntityAttributeModifier(identifier, armor, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(EntityAttributes.ARMOR_TOUGHNESS, new EntityAttributeModifier(identifier, 0.5, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY, new EntityAttributeModifier(identifier, 0.5, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(identifier, 1.0, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        return builder.build();
    }
}