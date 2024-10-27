package frootloops.versus.mod.items.equipment;

import frootloops.versus.VersusMod;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import static frootloops.versus.mod.Combat.*;

public abstract class RebalancedTools {

    public static final Identifier ATTACK_REACH_MODIFIER_ID = Identifier.of(VersusMod.MOD_ID,"attack_reach_modifier");

    public static final ToolMaterial WOOD = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 64, 4.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS);
    public static final ToolMaterial STONE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 128, 4.5F, 1.0F, 5, ItemTags.STONE_TOOL_MATERIALS);
    public static final ToolMaterial IRON = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 256, 6.0F, 2.0F, 14, ItemTags.IRON_TOOL_MATERIALS);
    public static final ToolMaterial GOLD = new ToolMaterial(BlockTags.INCORRECT_FOR_GOLD_TOOL, 48, 12.0F, 2.0F, 26, ItemTags.GOLD_TOOL_MATERIALS);
    public static final ToolMaterial DIAMOND = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1540, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS);
    public static final ToolMaterial NETHERITE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2056, 9.0F, 4.0F, 15, ItemTags.NETHERITE_TOOL_MATERIALS);

    public static ToolMaterial getRebalancedToolMaterial(ToolMaterial original) {
        if(original == ToolMaterial.WOOD)  return RebalancedTools.WOOD;
        if(original == ToolMaterial.STONE)  return RebalancedTools.STONE;
        if(original == ToolMaterial.IRON)  return RebalancedTools.IRON;
        if(original == ToolMaterial.GOLD)  return RebalancedTools.GOLD;
        if(original == ToolMaterial.DIAMOND)  return RebalancedTools.DIAMOND;
        if(original == ToolMaterial.NETHERITE)  return RebalancedTools.NETHERITE;
        return original;
    }

    public static final float getAxeSpeedModifier() { return 1.0f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getAxeDamageModifier() { return 6.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}

    public static final float getSwordSpeedModifier() { return 1.6f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getSwordDamageModifier() { return 3.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}
    public static final AttributeModifiersComponent getSwordReachModifier() {
        return AttributeModifiersComponent.builder().add(EntityAttributes.ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(ATTACK_REACH_MODIFIER_ID, 0.5, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND).build();
    }

    public static final float getHoeSpeedModifier() { return 2.0f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getHoeDamageModifier() { return 0.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}
    public static final AttributeModifiersComponent getHoeReachModifier() {
        return AttributeModifiersComponent.builder().add(EntityAttributes.ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(ATTACK_REACH_MODIFIER_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND).build();
    }

    public static final float getPickaxeSpeedModifier() { return 1.2f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getPickaxeDamageModifier() { return 2.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}

    public static final float getShovelSpeedModifier() { return 1.4f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getShovelDamageModifier() { return 2.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}

    public static final float getTridentSpeedModifier() { return 1.0f - (float)PLAYER_BASE_ATTACK_SPEED;}
    public static final float getTridentDamageModifier() { return 9.0f - (float)PLAYER_BASE_ATTACK_DAMAGE;}
    public static final double getTridentReachModifier() { return 1.0d;}

}