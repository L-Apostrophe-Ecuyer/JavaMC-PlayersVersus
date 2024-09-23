package frootloops.versus.mixin.items.equipment.tools;

import ItemStack;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class Deprecated_HoeItemMixin extends ToolItem {
    public Deprecated_HoeItemMixin(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        if(Enchants.hasEnchantment(stack, Enchantments.SWEEPING_EDGE)) return UseAction.BRUSH;
        return UseAction.BLOCK;
    }

    @Inject(method = "useOnBlock", at = @At("RETURN"), cancellable = true)
    private void sweepEvenWhenTilling(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if(!context.getPlayer().isSneaking()) {
            ItemStack stack = context.getStack();
            PlayerEntity user = context.getPlayer();
            int sweepLevel = Enchants.getLevel(user.getWorld(), stack, Enchantments.SWEEPING_EDGE);
            if(sweepLevel > 0) {
                double attackCharge = Combat.getAttackChargeProgress(user);
                double attackChargeRequired = 1d + 0.2d * (3 - sweepLevel);
                if (attackCharge >= attackChargeRequired) {
                    user.setCurrentHand(context.getHand());
                    Combat.doSpecialSweepAttack(user, Combat.getAttackRange(user, attackCharge), sweepLevel);
                }
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int sweepLevel = Enchants.getLevel(world, stack, Enchantments.SWEEPING_EDGE);
        if(sweepLevel > 0) {
            double attackCharge = Combat.getAttackChargeProgress(user);
            double attackChargeRequired = 1d + 0.2d * (3 - sweepLevel);
            if (attackCharge >= attackChargeRequired) {
                user.setCurrentHand(hand);
                Combat.doSpecialSweepAttack(user, Combat.getAttackRange(user, attackCharge), sweepLevel);
                return TypedActionResult.consume(stack);
            }
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if(Enchants.getLevel(user.getWorld(), stack, Enchantments.SWEEPING_EDGE) > 0) {
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
