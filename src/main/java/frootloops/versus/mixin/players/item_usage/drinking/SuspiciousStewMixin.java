package frootloops.versus.mixin.players.item_usage.drinking;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SuspiciousStewItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SuspiciousStewItem.class)
public class SuspiciousStewMixin extends Item {
    public SuspiciousStewMixin(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 16;
    }

    @Inject(method = "finishUsing", at=@At(value = "NEW", target = "net/minecraft/item/ItemStack"), cancellable = true)
    private void stackableStew(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir){
        ItemStack emptyBowl = new ItemStack(Items.BOWL);
        if(user instanceof PlayerEntity player && !player.getInventory().insertStack(emptyBowl))
            player.dropItem(emptyBowl, false);
        cir.setReturnValue(stack);
    }

    @ModifyVariable(method = "addEffectToStew", at = @At("HEAD"), ordinal = 0)
    private static int doubleDuration(int duration) {
        return duration >> 1;
    }
}

