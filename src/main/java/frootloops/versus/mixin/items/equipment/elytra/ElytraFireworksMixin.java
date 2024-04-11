package frootloops.versus.mixin.items.equipment.elytra;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
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
    private void fireworksTweak(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if(!world.isClient && user.isFallFlying() && !user.getAbilities().creativeMode && user instanceof ServerPlayerEntity serverPlayer) {
            ItemStack elytraStack = serverPlayer.getEquippedStack(EquipmentSlot.CHEST);
            if(elytraStack.isOf(Items.ELYTRA)) {

                // Durability of elytra is only ever affected by fireworks:
                int maxDamage = elytraStack.getMaxDamage();
                int durabilityLeft = maxDamage - elytraStack.getDamage();
                elytraStack.damage(Math.min(durabilityLeft - 1, 16), world.getRandom(), serverPlayer);
            }
        }
    }
}
