package frootloops.versus.mixin.items.throwing;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowballItem.class)
public class SnowballMixin extends Item {
    public SnowballMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("HEAD"))
    private void setSplashCooldown(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        user.getItemCooldownManager().set(user.getStackInHand(hand), 8);
    }
}