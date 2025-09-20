package frootloops.versus.mixin.items_and_effects.equipment.shields;

import frootloops.versus.mod.Combat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShieldItem.class)
public abstract class ShieldItemMixin extends Item {
    public ShieldItemMixin(net.minecraft.item.Item.Settings settings) { super(settings); }

    @ModifyVariable(method = "<init>",at = @At("HEAD"), ordinal = 0)
    private static net.minecraft.item.Item.Settings constructorMixin(net.minecraft.item.Item.Settings settings) {
        return settings.enchantable(4);
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        return false;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(user.getAttackCooldownProgress(0.0f) > 0.5f) {
            user.resetLastAttackedTicks();
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        else return ActionResult.FAIL;
    }
}
