package frootloops.versus.mod.items;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.equipment.CustomEquipment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static frootloops.versus.mod.items.equipment.CustomEquipment.*;

public class VanillaItemsV2 {

    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_MODDED = new HashMap<>();

    public static void onInitialize() {
        // Modify default components:
        DefaultItemComponentEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.of(VersusMod.MOD_ID, "late"));
        DefaultItemComponentEvents.MODIFY.register(context -> {

            // Modify tools:
            modifyVanillaToolsAndWeapons(context);

            // Modify stack sizes:
            modifyVanillaStackSizesAndFoods(context, 64, 8, 16, 64, 16, 64);

            // Add food component to glistering melon slices:
            context.modify(Items.GLISTERING_MELON_SLICE, builder -> {builder
                    .add(DataComponentTypes.FOOD, new FoodComponent.Builder().nutrition(4).saturationModifier(1.0F).build())
                    .add(DataComponentTypes.CONSUMABLE, createFoodConsumptionComponent(1.4F, true, new ApplyEffectsConsumeEffect(
                            List.of(new StatusEffectInstance(StatusEffects.REGENERATION, 10, 2))
                    )));
            });
        });
    }

    private static void setUpTransformVanillaItemsToModded() {
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.CHARCOAL, Items.COAL);

        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.FERMENTED_SPIDER_EYE, CustomBrewingItems.CORRUPTED_WART_POWDER);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.BLAZE_POWDER, CustomBrewingItems.CONCENTRATE_OF_STRENGTH);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MAGMA_CREAM, CustomBrewingItems.CONCENTRATE_OF_FIRE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.RECOVERY_COMPASS, CustomEquipment.RECOVERY_COMPASS);

        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.LADDER, CustomBlockItems.LADDER);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.CLAY, CustomBlockItems.GRAY_CLAY);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD, CustomBlockItems.GRAY_MUD);

        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.PACKED_MUD, CustomBlockItems.BROWN_CLAY);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICKS, CustomBlockItems.BROWN_CLAY_BRICKS);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_SLAB, CustomBlockItems.BROWN_CLAY_BRICK_SLAB);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_STAIRS, CustomBlockItems.BROWN_CLAY_BRICK_STAIRS);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_WALL, CustomBlockItems.BROWN_MUD_BRICK_WALL);
    }

    public static Item getReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.getOrDefault(item, item);
    }

    public static boolean hasReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.containsKey(item);
    }

    private static void modifyVanillaStackSizesAndFoods(final DefaultItemComponentEvents.ModifyContext context, final int maxFoods, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        VersusMod.MOD_LOGGER.info("Modifying vanilla stack sizes and food components...");
        for (Item item : Registries.ITEM) {
            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                modifyVanillaFoodItem(context, item, maxFoods, maxBottled, maxStews);
            }
            else {
                if(item.getMaxCount() == 1) {
                    if(item.getComponents().contains(DataComponentTypes.MAX_DAMAGE)) return;

                    EquippableComponent equipComponent = item.getComponents().getOrDefault(DataComponentTypes.EQUIPPABLE, null);
                    if (equipComponent != null && (equipComponent.slot() == EquipmentSlot.SADDLE || equipComponent.slot() == EquipmentSlot.BODY)) modifyVanillaStackSizeOf(context, item, maxPlaceableEntities);
                    else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem) modifyVanillaStackSizeOf(context, item, maxPlaceableEntities);
                }
                else if (item instanceof BlockItem) modifyVanillaStackSizeOf(context, item, maxPlaceableBlocks);
            }
        }

        // Throwables:
        modifyVanillaStackSizeOf(context, Items.EGG, maxThrowables);
        modifyVanillaStackSizeOf(context, Items.SNOWBALL, maxThrowables);
        modifyVanillaStackSizeOf(context, Items.ENDER_PEARL, maxThrowables);
        modifyVanillaStackSizeOf(context, Items.FIRE_CHARGE, maxThrowables);

        // Others:
        modifyVanillaStackSizeOf(context, Items.POTION, maxBottled);
        modifyVanillaStackSizeOf(context, Items.POWDER_SNOW_BUCKET, maxPlaceableEntities);
        modifyVanillaStackSizeOf(context, Items.CAKE, maxFoods);
        modifyVanillaStackSizeOf(context, Items.RECOVERY_COMPASS, 1);
    }

    private static void modifyVanillaStackSizeOf(final DefaultItemComponentEvents.ModifyContext context, Item item, int newStackSize) {
        if(item.getMaxCount() == newStackSize) return;
        context.modify(item, builder -> {builder.add(DataComponentTypes.MAX_STACK_SIZE, newStackSize);});
    }

    private static void modifyVanillaFoodItem(final DefaultItemComponentEvents.ModifyContext context, final Item item, final int maxFoods, final int maxBottled, final int maxStews) {
        FoodComponent foodComponent = item.getComponents().get(DataComponentTypes.FOOD);
        ConsumableComponent consumeComponent = item.getComponents().get(DataComponentTypes.CONSUMABLE);
        boolean isDrink = consumeComponent.sound() == SoundEvents.ENTITY_GENERIC_DRINK;
        boolean hasParticles = !isDrink;
        float useTimeSeconds = 1.6F;
        int maxCount = maxFoods;
        if(item == Items.RABBIT_STEW) {
            isDrink = false;
            hasParticles = false;
            useTimeSeconds = 0.8F;
            maxCount = maxStews;
        }
        else if(item.getComponents().contains(DataComponentTypes.USE_REMAINDER)) {
            Item useRemainder = item.getComponents().get(DataComponentTypes.USE_REMAINDER).convertInto().getItem();
            isDrink = true;
            hasParticles = false;
            useTimeSeconds = 0.8F;
            if(useRemainder == Items.GLASS_BOTTLE) maxCount = maxBottled;
            else if(useRemainder == Items.BOWL) maxCount = maxStews;
            else maxCount = Math.max(maxBottled, maxStews);
        }
        else {
            if(item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE || item == Items.PUFFERFISH || item == Items.POTATO || item == Items.POISONOUS_POTATO) useTimeSeconds = 2.0F;
            else if(item == Items.PUMPKIN_PIE || item == Items.COOKIE) useTimeSeconds = 1.0F;
            else if(item == Items.BREAD || (item instanceof BlockItem blockItem && blockItem.getBlock().getHardness() < 1.0F)) useTimeSeconds = 1.2F;
            else if(item.getTranslationKey().contains("cooked")) useTimeSeconds = 1.8F;
            else if(item.getTranslationKey().contains("raw")) useTimeSeconds = 2.0F;
            else if(foodComponent.nutrition() <= 3 && foodComponent.saturation() <= 0.8F) useTimeSeconds = 1.2F;
        }
        ConsumableComponent consumableComponent = isDrink ?
                createDrinkConsumptionComponent(useTimeSeconds, consumeComponent.onConsumeEffects()) :
                createFoodConsumptionComponent(useTimeSeconds, hasParticles, consumeComponent.onConsumeEffects());
        if(maxCount != item.getMaxCount()) modifyVanillaStackSizeOf(context, item, maxCount);
        context.modify(item, builder -> {builder.add(DataComponentTypes.CONSUMABLE, consumableComponent);});
    }


    private static void modifyVanillaToolsAndWeapons(final DefaultItemComponentEvents.ModifyContext context) {
        VersusMod.MOD_LOGGER.info("Modifying vanilla tools and armor...");
        modifyToolComponents(context, Items.TRIDENT, TRIDENT_DAMAGE, TRIDENT_SPEED, TRIDENT_REACH);

        // Shields are instant:
        context.modify(Items.SHIELD, builder -> {
            builder.add(DataComponentTypes.BLOCKS_ATTACKS, createDamageBlockingComponent(0.0625F, 1.0F, 0.0F, 1.0F, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK));
        });

        double extra = 0.0;
        modifySwordComponents(context, Items.WOODEN_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, (float)extra);
        modifyToolComponents(context, Items.WOODEN_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
        modifyToolComponents(context, Items.WOODEN_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
        modifyToolComponents(context, Items.WOODEN_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
        modifyToolComponents(context, Items.WOODEN_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

        extra = 1.0;
        modifySwordComponents(context, Items.STONE_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, (float)extra);
        modifyToolComponents(context, Items.STONE_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
        modifyToolComponents(context, Items.STONE_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
        modifyToolComponents(context, Items.STONE_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
        modifyToolComponents(context, Items.STONE_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

        extra = 2.0;
        modifySwordComponents(context, Items.IRON_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, (float)extra);
        modifyToolComponents(context, Items.IRON_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
        modifyToolComponents(context, Items.IRON_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
        modifyToolComponents(context, Items.IRON_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
        modifyToolComponents(context, Items.IRON_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

        extra = 3.0;
        modifySwordComponents(context, Items.DIAMOND_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, (float)extra);
        modifyToolComponents(context, Items.DIAMOND_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
        modifyToolComponents(context, Items.DIAMOND_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
        modifyToolComponents(context, Items.DIAMOND_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
        modifyToolComponents(context, Items.DIAMOND_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);

        extra = 4.0;
        modifySwordComponents(context, Items.NETHERITE_SWORD, SWORD_DAMAGE + extra, SWORD_SPEED, SWORD_REACH, (float)extra);
        modifyToolComponents(context, Items.NETHERITE_SHOVEL, SHOVEL_DAMAGE + extra, SHOVEL_SPEED, SHOVEL_REACH);
        modifyToolComponents(context, Items.NETHERITE_PICKAXE, PICKAXE_DAMAGE + extra, PICKAXE_SPEED, PICKAXE_REACH);
        modifyToolComponents(context, Items.NETHERITE_AXE, AXE_DAMAGE + extra, AXE_SPEED, AXE_REACH);
        modifyToolComponents(context, Items.NETHERITE_HOE, HOE_DAMAGE + extra, HOE_SPEED, HOE_REACH);
    }

    private static void modifySwordComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, float baseBlockingAmount) {
        context.modify(item, builder -> {builder
                .add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange))
                .add(DataComponentTypes.BLOCKS_ATTACKS, getSwordBlockingComponent(baseBlockingAmount, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK));
        });
    }

    private static void modifyToolComponents(final DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange) {
        context.modify(item, builder -> {
            builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange));
        });
    }

    public static ConsumableComponent createDrinkConsumptionComponent(float consumeSeconds, List<ConsumeEffect> consumeEffects) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(consumeSeconds).useAction(UseAction.DRINK).sound(SoundEvents.ENTITY_GENERIC_DRINK).consumeParticles(false);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.consumeEffect(effect);
        return consumeComponent.build();
    }

    public static ConsumableComponent createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, ApplyEffectsConsumeEffect applyEffectsConsumeEffect) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(1.6F).useAction(UseAction.EAT).sound(SoundEvents.ENTITY_GENERIC_EAT).consumeParticles(true);
        if(applyEffectsConsumeEffect != null) consumeComponent.consumeEffect(applyEffectsConsumeEffect);
        return consumeComponent.build();
    }
    public static ConsumableComponent createFoodConsumptionComponent(float consumeSeconds, boolean doParticles, List<ConsumeEffect> consumeEffects) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().consumeSeconds(1.6F).useAction(UseAction.EAT).sound(SoundEvents.ENTITY_GENERIC_EAT).consumeParticles(true);
        for(ConsumeEffect effect : consumeEffects) consumeComponent.consumeEffect(effect);
        return consumeComponent.build();
    }

    public static ConsumableComponent createUseActionComponent(UseAction useAction, float consumeSeconds, RegistryEntry<SoundEvent> sound, RegistryEntry<SoundEvent> finishSound, boolean consumeParticles, ConsumeEffect consumeEffect) {
        ConsumableComponent.Builder consumeComponent = ConsumableComponent.builder().useAction(useAction).consumeSeconds(consumeSeconds).sound(sound).finishSound(finishSound).consumeParticles(consumeParticles).consumeEffect(consumeEffect);
        return consumeComponent.build();
    }
}