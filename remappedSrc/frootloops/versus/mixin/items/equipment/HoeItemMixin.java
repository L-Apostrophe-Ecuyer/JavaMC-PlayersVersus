package frootloops.versus.mixin.items.equipment;

import frootloops.versus.mod.Combat;
import net.minecraft.enchantment.EnchantmentHelper;
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
public class HoeItemMixin extends ToolItem {
    public HoeItemMixin(ToolMaterial material, net.minecraft.item.Item.Settings settings) {
        super(material, settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        int sweepLevel = EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack);
        return sweepLevel > 0 ? UseAction.BRUSH : UseAction.NONE;
    }

    @Inject(method = "useOnBlock", at = @At("RETURN"), cancellable = true)
    private void sweepEvenWhenTilling(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if(!context.getPlayer().isSneaking()) {
            ItemStack stack = context.getStack();
            PlayerEntity user = context.getPlayer();
            int sweepLevel = EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack);
            if(sweepLevel > 0) {
                double attackCharge = Combat.getAttackChargeProgress(user);
                double attackChargeRequired = 1d + 0.2d * (3 - sweepLevel);
                if (attackCharge >= attackChargeRequired) {
                    user.setCurrentHand(context.getHand());
                    Combat.doSweepAttack(user, Combat.getAttackRange(user, attackCharge), sweepLevel);
                }
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int sweepLevel = EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack);
        if(sweepLevel > 0) {
            double attackCharge = Combat.getAttackChargeProgress(user);
            double attackChargeRequired = 1d + 0.2d * (3 - sweepLevel);
            if (attackCharge >= attackChargeRequired) {
                user.setCurrentHand(hand);
                Combat.doSweepAttack(user, Combat.getAttackRange(user, attackCharge), sweepLevel);
                return TypedActionResult.consume(stack);
            }
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 6;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if((EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack) > 0)) {
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
