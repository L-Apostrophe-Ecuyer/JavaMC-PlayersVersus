package frootloops.versus.mod.items;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class VanillaItemsV2 implements ModInitializer {

    public static final Identifier ATTACK_REACH_MODIFIER_ID = Identifier.of(VersusMod.MOD_ID,"attack_reach_modifier");
    private static final double TRIDENT_SPEED = 1.0, TRIDENT_DAMAGE = 9.0, TRIDENT_REACH = 1.0;
    private static final double PICKAXE_SPEED = 1.2, PICKAXE_DAMAGE = 2.0, PICKAXE_REACH = 0.0;
    private static final double SHOVEL_SPEED = 1.4, SHOVEL_DAMAGE = 3.0, SHOVEL_REACH = 0.0;
    private static final double SWORD_SPEED = 1.6, SWORD_DAMAGE = 3.0, SWORD_REACH = 0.5;
    private static final double HOE_SPEED = 2.0, HOE_DAMAGE = 1.0, HOE_REACH = 1.0;
    private static final double AXE_SPEED = 1.0, AXE_DAMAGE = 6.0, AXE_REACH = 0.0;

    @Override
    public void onInitialize() {
        Identifier latePhase = Identifier.of(VersusMod.MOD_ID, "late");
        DefaultItemComponentEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, latePhase);

        // Modify default components:
        DefaultItemComponentEvents.MODIFY.register(context -> {

            // Modify tools:
            modifyVanillaToolsAndWeapons(context);

            // Modify stack sizes:


            // Add food component to glistering melon slices:
            context.modify(Items.GLISTERING_MELON_SLICE, builder -> {builder.add(
                    DataComponentTypes.FOOD, null
            );});

        });
    }





    private static void modifyVanillaToolsAndWeapons(DefaultItemComponentEvents.ModifyContext context) {
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

    private static void modifySwordComponents(DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange, float baseBlockingAmount) {
        context.modify(item, builder -> {builder
                .add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange))
                .add(DataComponentTypes.BLOCKS_ATTACKS, createDamageBlockingComponent(0.0625F, 0.5F, baseBlockingAmount, 0.5F, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK));
        });
    }

    private static void modifyToolComponents(DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange) {
        context.modify(item, builder -> {
            builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange));
        });
    }

    private static BlocksAttacksComponent createDamageBlockingComponent(
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

    private static AttributeModifiersComponent createToolAttributeModifiers(double attackDamage, double attackSpeed, double extraAttackRange) {
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