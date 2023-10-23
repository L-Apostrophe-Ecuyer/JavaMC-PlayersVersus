package frootloops.versus.mixin.items.elytra;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FireworkRocketItem.class)
public class ElytraFireworksMixin extends Item {

    public ElytraFireworksMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void addAttackReachTooltip(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if(!world.isClient && user.isFallFlying() && !user.getAbilities().creativeMode) {
            ItemStack elytraStack = user.getEquippedStack(EquipmentSlot.CHEST);
            if(elytraStack.isOf(Items.ELYTRA)) {
                int maxDamage = elytraStack.getMaxDamage();
                for (int i = 0; i < 3; i++) {
                    int durabilityLeft = maxDamage - elytraStack.getDamage();
                    if(durabilityLeft > 1) {
                        elytraStack.damage(Math.min(durabilityLeft - 1, 3), user, p -> p.sendEquipmentBreakStatus(EquipmentSlot.CHEST));
                    }
                    else {
                        break;
                    }
                }
            }
        }
    }
}
