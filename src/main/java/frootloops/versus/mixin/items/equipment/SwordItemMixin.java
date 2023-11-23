package frootloops.versus.mixin.items.equipment;

import frootloops.versus.mod.Combat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SwordItem.class)
public class SwordItemMixin extends ToolItem {
    public SwordItemMixin(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        int sweepLevel = EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack);
        return sweepLevel > 0 ? UseAction.BRUSH : UseAction.BLOCK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int sweepLevel = EnchantmentHelper.getLevel(Enchantments.SWEEPING, stack);
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
    public int getMaxUseTime(ItemStack stack) {
        return this.getUseAction(stack) == UseAction.BLOCK ? 72000 : 6;
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
