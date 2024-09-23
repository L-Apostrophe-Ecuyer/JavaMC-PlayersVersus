package frootloops.versus.mixin.items.equipment.tools;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public class SwordItemMixin extends ToolItem {

    public SwordItemMixin(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Inject(method = "createAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private static void createAttributeModifiers(ToolMaterial material, int baseAttackDamage, float attackSpeed, CallbackInfoReturnable<AttributeModifiersComponent> cir) {
        baseAttackDamage = (int)Combat.getSwordDamageModifier();
        attackSpeed = Combat.getSwordSpeedModifier();
        float attackReachBonus = Combat.getSwordReachModifier();
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

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if(world.getTime() % 4L != 0) return;
        if(user.isPlayer() && stack.getUseAction() == UseAction.BLOCK) {
            ItemStack otherHandStack = user.getActiveHand() == Hand.MAIN_HAND ? user.getOffHandStack() : user.getMainHandStack();
            if(otherHandStack.getItem() instanceof ShieldItem && otherHandStack.getUseAction() == UseAction.BLOCK) {
                if(((PlayerEntity)user).getItemCooldownManager().isCoolingDown(otherHandStack.getItem())) return;
                user.stopUsingItem();
                ((PlayerEntity)user).setCurrentHand(user.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
            }
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        if(Enchants.hasEnchantment(stack, Enchantments.SWEEPING_EDGE)) return UseAction.NONE;
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        if(Enchants.hasEnchantment(stack, Enchantments.SWEEPING_EDGE)) return 0;
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if(Enchants.hasEnchantment(stack, Enchantments.SWEEPING_EDGE)) return TypedActionResult.fail(stack);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    /*
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int sweepLevel = Enchants.getLevel(world, stack, Enchantments.SWEEPING_EDGE);
        double attackCharge = Combat.getAttackChargeProgress(user);
        double attackChargeRequired = sweepLevel > 0 ? 1d + 0.2d * (3 - sweepLevel) : 1d;
        if(attackCharge >= attackChargeRequired) {
            user.setCurrentHand(hand);
            Combat.doSpecialSweepAttack(user, Combat.getAttackRange(user,attackCharge), sweepLevel);
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if(stack.hasEnchantments() && Enchants.getLevel(world, stack, Enchantments.SWEEPING_EDGE) > 0) {
            if (user instanceof PlayerEntity player) player.resetLastAttackedTicks();
            else user.onAttacking(user.getAttacking());
        }
    }*/

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) player.resetLastAttackedTicks();
        else user.onAttacking(user.getAttacking());
        return stack;
    }

}
