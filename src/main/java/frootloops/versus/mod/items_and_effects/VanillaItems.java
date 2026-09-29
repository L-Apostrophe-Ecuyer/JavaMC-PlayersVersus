package frootloops.versus.mod.items_and_effects;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
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
        DefaultItemComponentEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "late"));

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
                    .set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(4).saturationModifier(1.0F).build())
                    .set(DataComponents.CONSUMABLE, createFoodConsumptionComponent(1.4F, true, new ApplyStatusEffectsConsumeEffect(
                            List.of(new MobEffectInstance(MobEffects.REGENERATION, 10, 2))
                    )));
            });

            // Give leather armor some knockback resistance:
            context.modify(Items.LEATHER_CHESTPLATE, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.CHESTPLATE, 2.0, 0.0, 0.1));});
            context.modify(Items.LEATHER_LEGGINGS, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.LEGGINGS, 2.0, 0.0, 0.05));});
            context.modify(Items.LEATHER_BOOTS, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.BOOTS, 1.0, 0.0, 0.0, -0.2));});

            // Give chainmail armor some toughness:
            context.modify(Items.CHAINMAIL_CHESTPLATE, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.CHESTPLATE, 5.0, 3.0, 0.0));});
            context.modify(Items.CHAINMAIL_LEGGINGS, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.LEGGINGS, 4.0, 2.0, 0.0));});
            context.modify(Items.CHAINMAIL_BOOTS, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.BOOTS, 2.0, 2.0, 0.0));});
            context.modify(Items.CHAINMAIL_HELMET, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createArmorAttributes(ArmorType.HELMET, 2.0, 2.0, 0.0));});

            // Give turtle helmets more buffs:
            context.modify(Items.TURTLE_HELMET, builder -> {builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createTurtleArmorAttributes(ArmorType.HELMET, 2.0));});
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
            for (Item item : BuiltInRegistries.ITEM) {
                if(item.getDefaultMaxStackSize() == 1) {
                    if (item.getDefaultInstance().getComponents().has(DataComponents.MAX_DAMAGE)) return;
                    Equippable equipComponent = item.getDefaultInstance().getComponents().getOrDefault(DataComponents.EQUIPPABLE, null);
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
        if(item.getDefaultInstance().getMaxStackSize() == newStackSize) return;
        context.modify(item, builder -> {builder.set(DataComponents.MAX_STACK_SIZE, newStackSize + 0);});
    }

    private static void modifyVanillaFoodItem(final Item item, final float eatTime) {
        UseRemainder remainderComponent = item.getDefaultInstance().getOrDefault(DataComponents.USE_REMAINDER, null);
        boolean isHoneyBottle = item == Items.HONEY_BOTTLE;
        boolean isBottled = !isHoneyBottle && (remainderComponent != null && remainderComponent.convertInto().is(Items.GLASS_BOTTLE));
        boolean isBucket = !isBottled && remainderComponent != null && remainderComponent.convertInto().is(Items.BUCKET);
        boolean isStew = !isBucket && !isBottled && remainderComponent != null && remainderComponent.convertInto().is(Items.BOWL);

        Consumable consumeComponent = item.getDefaultInstance().getOrDefault(DataComponents.CONSUMABLE, null);
        boolean isDrink = isHoneyBottle || isStew || isBucket || isBottled || consumeComponent.sound() == SoundEvents.GENERIC_DRINK;
        boolean hasParticles = !isDrink && remainderComponent == null;

        Consumable newConsumeComponent = isDrink ?
                createDrinkConsumptionComponent(eatTime, consumeComponent.onConsumeEffects(), isHoneyBottle) :
                createFoodConsumptionComponent(eatTime, hasParticles, consumeComponent.onConsumeEffects());

        final int maxCount;
        if(isBottled) maxCount = VersusSettings.Items.MAX_COUNT_BOTTLED;
        else if(isStew) maxCount = VersusSettings.Items.MAX_COUNT_STEWS;
        else if(isBucket) maxCount = VersusSettings.Items.MAX_COUNT_BUCKETS;
        else maxCount = VersusSettings.Items.MAX_COUNT_FOOD;
        DefaultItemComponentEvents.MODIFY.register(context -> {
            if(maxCount != item.getDefaultInstance().getMaxStackSize()) modifyVanillaStackSizeOf(context, item, maxCount);
            context.modify(item, builder -> {builder.set(DataComponents.CONSUMABLE, newConsumeComponent);});
        });
    }


    private static void modifyVanillaToolsAndWeapons() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            modifyToolComponents(context, Items.TRIDENT, TRIDENT_DAMAGE, TRIDENT_SPEED, TRIDENT_REACH);

            // Shields are instant:
            context.modify(Items.SHIELD, builder -> {
                builder.set(DataComponents.BLOCKS_ATTACKS, createDamageBlockingComponent(0.0625F, 1.0F, 5.0F, 0.8F, SoundEvents.SHIELD_BLOCK, SoundEvents.SHIELD_BREAK));
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

    private static void modifySwordComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, float baseBlockingAmount, Holder.Reference<SoundEvent> blockingSound) {
        context.modify(item, builder -> {builder
                .set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange, 0.0))
                .set(DataComponents.BLOCKS_ATTACKS, getSwordBlockingComponent(baseBlockingAmount, blockingSound, blockingSound));
        });
    }

    private static void modifyToolComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange) {
        modifyToolComponents(context, item, attackDamage, attackSpeed, extraAttackRange, 0.0);
    }

    private static void modifyToolComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, double extraAttackKnockback) {
        context.modify(item, builder -> {
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange, extraAttackKnockback));
        });
    }

    public static Consumable createDrinkConsumptionComponent(float consumeSeconds, List<ConsumeEffect> consumeEffects, boolean usHoneySound) {
        Consumable.Builder consumeComponent = Consumable.builder().consumeSeconds(consumeSeconds).animation(ItemUseAnimation.DRINK).sound(usHoneySound ? SoundEvents.HONEY_DRINK : SoundEvents.GENERIC_DRINK).hasConsumeParticles(false);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.onConsume(effect);
        return consumeComponent.build();
    }

    public static Consumable createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, ApplyStatusEffectsConsumeEffect applyEffectsConsumeEffect) {
        Consumable.Builder consumeComponent = Consumable.builder().consumeSeconds(consumeSeconds).animation(ItemUseAnimation.EAT).sound(SoundEvents.GENERIC_EAT).hasConsumeParticles(doParticles);
        if(applyEffectsConsumeEffect != null) consumeComponent.onConsume(applyEffectsConsumeEffect);
        return consumeComponent.build();
    }
    public static Consumable createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, List<ConsumeEffect> consumeEffects) {
        Consumable.Builder consumeComponent = Consumable.builder().consumeSeconds(consumeSeconds).animation(ItemUseAnimation.EAT).sound(SoundEvents.GENERIC_EAT).hasConsumeParticles(doParticles);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.onConsume(effect);
        return consumeComponent.build();
    }

    public static Consumable createUseActionComponent(ItemUseAnimation useAction, float consumeSeconds, Holder<SoundEvent> sound, Holder<SoundEvent> finishSound, boolean consumeParticles, ConsumeEffect consumeEffect) {
        Consumable.Builder consumeComponent = Consumable.builder().animation(useAction).consumeSeconds(consumeSeconds).sound(sound).soundAfterConsume(finishSound).hasConsumeParticles(consumeParticles).onConsume(consumeEffect);
        return consumeComponent.build();
    }

    public static BlocksAttacks getSwordBlockingComponent(float baseBlockingAmount, Holder.Reference<SoundEvent> soundBlocking, Holder.Reference<SoundEvent> soundBreaking) {
        return createDamageBlockingComponent(0.0625F, 0.5F, Math.max(0F, baseBlockingAmount), 0.5F, soundBlocking, soundBreaking);
    }


    public static BlocksAttacks createDamageBlockingComponent(
            float blockDelaySeconds,    // The amount of time (in seconds) that use must be held before successfully blocking attacks
            float disableCooldownScale, // The multiplier applied to the cooldown time for the item when attacked by a disabling attack
            float amountBlockedBase,    // The constant amount of damage to be blocked
            float amountBlockedFactor,  // The fraction of the dealt damage to be blocked
            Holder.Reference<SoundEvent> soundBlocking, Holder.Reference<SoundEvent> soundBreaking
    ) {
        float horizontalBlockingAngle = 90F;
        float itemDamageThreshold = 3.0F;
        float itemDamageBase = 1.0F;
        float itemDamageFactor = 1.0F;
        return new BlocksAttacks(
                blockDelaySeconds,
                disableCooldownScale,
                List.of(new BlocksAttacks.DamageReduction(horizontalBlockingAngle, Optional.empty(), amountBlockedBase, amountBlockedFactor)),
                new BlocksAttacks.ItemDamageFunction(itemDamageThreshold, itemDamageBase, itemDamageFactor),
                Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                Optional.of(soundBlocking),
                Optional.of(soundBreaking)
        );
    }

    public static ItemAttributeModifiers createToolAttributeModifiers(double attackDamage, double attackSpeed, double extraAttackRange, double extraAttackKnockback) {
        ItemAttributeModifiers.Builder attributeBuilder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage - Combat.PLAYER_BASE_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed - Combat.PLAYER_BASE_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        if(extraAttackRange != 0.0)
            attributeBuilder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ATTACK_REACH_MODIFIER_ID, extraAttackRange, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        if(extraAttackKnockback != 0.0)
            attributeBuilder.add(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(ATTACK_KNOCKBACK_MODIFIER_ID, extraAttackRange, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        return attributeBuilder.build();
    }


    public static ItemAttributeModifiers createArmorAttributes(ArmorType type, double armor, double toughness, double kbResistance) {
        return createArmorAttributes(type, armor, toughness, kbResistance, 0.0);
    }
    public static ItemAttributeModifiers createArmorAttributes(ArmorType type, double armor, double toughness, double kbResistance, double fallDmgMultiplier) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup attributeModifierSlot = EquipmentSlotGroup.bySlot(type.getSlot());
        Identifier identifier = Identifier.withDefaultNamespace("armor." + type.getName());

        builder.add(Attributes.ARMOR, new AttributeModifier(identifier, armor, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (toughness != 0.0) builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(identifier, toughness, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (kbResistance > 0.0) builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(identifier, kbResistance, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        if (fallDmgMultiplier != 0.0) builder.add(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(identifier, fallDmgMultiplier, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);

        return builder.build();
    }

    private static ItemAttributeModifiers createTurtleArmorAttributes(ArmorType type, double armor) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup attributeModifierSlot = EquipmentSlotGroup.bySlot(type.getSlot());
        Identifier identifier = Identifier.withDefaultNamespace("armor." + type.getName());

        builder.add(Attributes.ARMOR, new AttributeModifier(identifier, armor, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(identifier, 0.5, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(Attributes.WATER_MOVEMENT_EFFICIENCY, new AttributeModifier(identifier, 0.5, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(identifier, 1.0, AttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        return builder.build();
    }
}