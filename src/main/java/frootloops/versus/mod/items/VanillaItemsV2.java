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

import static frootloops.versus.mod.items.equipment.CustomEquipment.*;

public class VanillaItemsV2 implements ModInitializer {

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
                .add(DataComponentTypes.BLOCKS_ATTACKS, getSwordBlockingComponent(baseBlockingAmount, SoundEvents.ITEM_SHIELD_BLOCK, SoundEvents.ITEM_SHIELD_BREAK));
        });
    }

    private static void modifyToolComponents(DefaultItemComponentEvents.ModifyContext context, Item item, double attackDamage, double attackSpeed, double extraAttackRange) {
        context.modify(item, builder -> {
            builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, createToolAttributeModifiers(attackDamage, attackSpeed, extraAttackRange));
        });
    }






}