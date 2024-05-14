package frootloops.versus.mixin.items.equipment.tools;

import frootloops.versus.mod.Combat;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public class SwordItemMixin extends ToolItem {
    public SwordItemMixin(ToolMaterial material, net.minecraft.item.Item.Settings settings) {
        super(material, settings);
    }

    @Inject(method = "createAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private static void createAttributeModifiers(ToolMaterial material, int baseAttackDamage, float attackSpeed, CallbackInfoReturnable<AttributeModifiersComponent> cir) {
        baseAttackDamage = (int)Combat.getSwordDamageModifier();
        attackSpeed = Combat.getSwordSpeedModifier();
        float attackReachBonus = Combat.getSwordReachModifier();
        if(attackReachBonus != 0.0f) cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Combat.ATTACK_REACH_MODIFIER_ID, "Weapon modifier", attackReachBonus, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        else cir.setReturnValue(AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", baseAttackDamage + material.getAttackDamage(), EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build());
        cir.cancel();
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        int sweepLevel = EnchantmentHelper.getLevel((RegistryEntry<Enchantment>) Enchantments.SWEEPING_EDGE, stack);
        return sweepLevel > 0 ? UseAction.BRUSH : UseAction.BLOCK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int sweepLevel = EnchantmentHelper.getLevel((RegistryEntry<Enchantment>) Enchantments.SWEEPING_EDGE, stack);
        double attackCharge = Combat.getAttackChargeProgress(user);
        double attackChargeRequired = sweepLevel > 0 ? 1d + 0.2d * (3 - sweepLevel) : 1d;
        if(attackCharge >= attackChargeRequired) {
            user.setCurrentHand(hand);
            Combat.doSweepAttack(user, Combat.getAttackRange(user,attackCharge), sweepLevel);
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if((EnchantmentHelper.getLevel((RegistryEntry<Enchantment>) Enchantments.SWEEPING_EDGE, stack) > 0)) {
            if (user instanceof PlayerEntity player) player.resetLastAttackedTicks();
            else user.onAttacking(user.getAttacking());
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) player.resetLastAttackedTicks();
        else user.onAttacking(user.getAttacking());
        return stack;
    }

}
