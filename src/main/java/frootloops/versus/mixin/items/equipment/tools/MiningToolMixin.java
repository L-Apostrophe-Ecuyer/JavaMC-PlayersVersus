package frootloops.versus.mixin.items.equipment.tools;

import frootloops.versus.VersusMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.item.*;
import net.minecraft.sound.BlockSoundGroup;
import frootloops.versus.mod.Combat;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.item.Item.BASE_ATTACK_DAMAGE_MODIFIER_ID;
import static net.minecraft.item.Item.BASE_ATTACK_SPEED_MODIFIER_ID;

@Mixin(MiningToolItem.class)
public abstract class MiningToolMixin extends ToolItem {

    public MiningToolMixin(ToolMaterial material, Settings settings) {
        super(material, settings);
    }


    @Inject(method = "createAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private static void createAttributeModifiers(ToolMaterial material, float baseAttackDamage, float attackSpeed, CallbackInfoReturnable<AttributeModifiersComponent> cir) {

        float attackReachBonus = 0.0f;
        if (attackSpeed == -3.0f & baseAttackDamage == 1.5f) {
            baseAttackDamage = Combat.getShovelDamageModifier();
            attackSpeed = Combat.getShovelSpeedModifier();
            attackReachBonus = Combat.getShovelReachModifier();

        } else if (attackSpeed == -2.8f && baseAttackDamage == 1.0f) {
            baseAttackDamage = Combat.getPickaxeDamageModifier();
            attackSpeed = Combat.getPickaxeSpeedModifier();

        } else if (attackSpeed >= -3.2f && attackSpeed <= -3.0f && baseAttackDamage <= 7.0f && baseAttackDamage >= 5.0f) {
            baseAttackDamage = Combat.getAxeDamageModifier();
            attackSpeed = Combat.getAxeSpeedModifier();

        } else if ((material == ToolMaterials.WOOD || material == ToolMaterials.GOLD) && attackSpeed == -3.0f && baseAttackDamage == 0.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeReachModifier();

        } else if ((material == ToolMaterials.STONE || material == ToolMaterials.IRON || material == ToolMaterials.DIAMOND) && attackSpeed + baseAttackDamage == -3.0f && baseAttackDamage <= -1.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeReachModifier();

        } else if ((material == ToolMaterials.NETHERITE) && baseAttackDamage == -4.0f && attackSpeed == 0.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeReachModifier();
        }
        else {
            VersusMod.MOD_LOGGER.warn("Tool wasn't registered: Damage of " + baseAttackDamage + " and speed of " + attackSpeed);
        }

        if(material == ToolMaterials.GOLD) baseAttackDamage += 2.0f;
        if (attackReachBonus != 0.0f) cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Combat.ATTACK_REACH_MODIFIER_ID, attackReachBonus, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        else cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        cir.cancel();
    }

    /*
    @Override
    public boolean isCorrectForDrops(ItemStack stack, BlockState state) {
        if(!super.isCorrectForDrops(stack, state)) {
            if(stack.getItem() instanceof PickaxeItem && (state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.SUSPICIOUS_GRAVEL) || state.getSoundGroup() == BlockSoundGroup.STONE || state.getSoundGroup() == BlockSoundGroup.MUD_BRICKS)) return true;
            else if(stack.getItem() instanceof ShovelItem && (state.isOf(Blocks.PACKED_MUD) || state.getSoundGroup() == BlockSoundGroup.MUD_BRICKS || state.getSoundGroup() == BlockSoundGroup.MUD)) return true;
            return false;
        }
        return true;
    }


    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        float miningSpeed = super.getMiningSpeed(stack,state);
        if(miningSpeed == 1.0f) {

            if(stack.getItem() instanceof PickaxeItem && state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.SUSPICIOUS_GRAVEL) || state.getSoundGroup() == BlockSoundGroup.STONE) {
                return stack.get(DataComponentTypes.TOOL).getSpeed(Blocks.STONE.getDefaultState());
            }

            else if(stack.getItem() instanceof ShovelItem && state.isOf(Blocks.PACKED_MUD) || state.getSoundGroup() == BlockSoundGroup.MUD_BRICKS) {
                return stack.get(DataComponentTypes.TOOL).getSpeed(Blocks.DIRT.getDefaultState());
            }
            return 1.0f;
        }
        return miningSpeed;
    }*/

}
