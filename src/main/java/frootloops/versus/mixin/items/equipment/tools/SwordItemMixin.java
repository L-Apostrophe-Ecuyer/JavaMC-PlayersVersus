package frootloops.versus.mixin.items.equipment.tools;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.items.equipment.RebalancedTools;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public abstract class SwordItemMixin extends Item {

    public SwordItemMixin(Settings settings) {
        super(settings);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static ToolMaterial modifyToolMaterial(ToolMaterial vanillaMaterial) {
        return RebalancedTools.getRebalancedToolMaterial(vanillaMaterial);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static float modifyAttackDamage(float dmg) {
        return RebalancedTools.getSwordDamageModifier();
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1)
    private static float modifyAttackSpeed(float speed) {
        return RebalancedTools.getSwordSpeedModifier();
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if(world.getTime() % 4L != 0) return;
        if(user.isPlayer() && stack.getUseAction() == UseAction.BLOCK) {
            ItemStack otherHandStack = user.getActiveHand() == Hand.MAIN_HAND ? user.getOffHandStack() : user.getMainHandStack();
            if(otherHandStack.getItem() instanceof ShieldItem && otherHandStack.getUseAction() == UseAction.BLOCK) {
                if(((PlayerEntity)user).getItemCooldownManager().isCoolingDown(otherHandStack)) return;
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
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if(Enchants.hasEnchantment(stack, Enchantments.SWEEPING_EDGE)) return ActionResult.FAIL;
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) player.resetLastAttackedTicks();
        else user.onAttacking(user.getAttacking());
        return stack;
    }

}
