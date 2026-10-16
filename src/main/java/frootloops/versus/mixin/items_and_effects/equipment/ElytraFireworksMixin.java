package frootloops.versus.mixin.items_and_effects.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FireworkRocketItem.class)
public class ElytraFireworksMixin extends Item {

    public ElytraFireworksMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void fireworksTweak(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if(!world.isClientSide() && user.isFallFlying() && !user.getAbilities().instabuild) {
            ItemStack elytraStack = user.getItemBySlot(EquipmentSlot.CHEST);
            if(elytraStack.is(Items.ELYTRA)) {

                // Durability of elytra is now mainly affected by fireworks, to incentivize gliding:
                int maxDamage = elytraStack.getMaxDamage();
                int durabilityLeft = maxDamage - elytraStack.getDamageValue();
                elytraStack.hurtAndBreak(Math.min(durabilityLeft - 1, 8), user, EquipmentSlot.CHEST);
            }
        }
    }
}
