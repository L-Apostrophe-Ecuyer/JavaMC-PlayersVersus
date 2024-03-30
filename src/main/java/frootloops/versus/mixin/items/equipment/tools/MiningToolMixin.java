package frootloops.versus.mixin.items.equipment.tools;

import frootloops.versus.mod.Combat;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.item.Item.ATTACK_DAMAGE_MODIFIER_ID;
import static net.minecraft.item.Item.ATTACK_SPEED_MODIFIER_ID;

@Mixin(MiningToolItem.class)
public class MiningToolMixin {


    @Inject(method = "createAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private static void createAttributeModifiers(ToolMaterial material, float baseAttackDamage, float attackSpeed, CallbackInfoReturnable<AttributeModifiersComponent> cir) {

        float attackReachBonus = 0.0f;

        if(baseAttackDamage == 1.5f && attackSpeed == -3.0f) {
            baseAttackDamage = Combat.getShovelDamageModifier();
            attackSpeed = Combat.getShovelSpeedModifier();
        }

        else if(baseAttackDamage == 1.5f && attackSpeed == -2.8f) {
            baseAttackDamage = Combat.getPickaxeDamageModifier();
            attackSpeed = Combat.getPickaxeSpeedModifier();
        }

        else if(baseAttackDamage == 1.5f && attackSpeed == -2.8f) {
            baseAttackDamage = Combat.getPickaxeDamageModifier();
            attackSpeed = Combat.getPickaxeSpeedModifier();
        }

        else if((attackSpeed <= -3.2f && attackSpeed >= -3.0f) && (baseAttackDamage <= 7.0f || baseAttackDamage >= 5.0f)) {
            baseAttackDamage = Combat.getAxeDamageModifier();
            attackSpeed = Combat.getAxeSpeedModifier();
        }

        else if((material == ToolMaterials.WOOD || material == ToolMaterials.GOLD) && attackSpeed == -3.0f && baseAttackDamage == 0.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeReachModifier();
        }

        else if((material == ToolMaterials.STONE || material == ToolMaterials.IRON || material == ToolMaterials.DIAMOND) && attackSpeed + baseAttackDamage == -3.0f && baseAttackDamage <= -1.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeReachModifier();
        }

        else if((material == ToolMaterials.NETHERITE) && baseAttackDamage == -4.0f && attackSpeed == 0.0f) {
            baseAttackDamage = Combat.getHoeDamageModifier();
            attackSpeed = Combat.getHoeSpeedModifier();
            attackReachBonus = Combat.getHoeSpeedModifier();
        }

        if(attackReachBonus != 0.0f) cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Combat.ATTACK_REACH_MODIFIER_ID, "Tool modifier", attackReachBonus, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        else cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        cir.cancel();
    }

}
