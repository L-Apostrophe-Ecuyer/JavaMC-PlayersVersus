package frootloops.versus.mixin.items.equipment.shields;

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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldItem.class)
public abstract class ShieldItemMixin extends Item {
    public ShieldItemMixin(Settings settings) { super(settings); }

    @Inject(method = "<init>",at = @At("HEAD"), cancellable = false)
    public void constructorMixin(Settings settings, CallbackInfo info) {
        settings.enchantable(4);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if(user instanceof PlayerEntity player) player.getItemCooldownManager().set(stack, 4);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if(Combat.getAttackChargeProgress(user) >= 0.5d) {
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        else return ActionResult.FAIL;
    }
}
