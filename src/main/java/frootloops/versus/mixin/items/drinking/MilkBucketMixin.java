package frootloops.versus.mixin.items.drinking;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MilkBucketItem.class)
public class MilkBucketMixin extends Item {
    public MilkBucketMixin(Settings settings) {
        super(settings);
    }
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 16;
    }

    @Inject(method = "finishUsing", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/item/ItemStack;decrement(I)V"), cancellable = true)
    private void stackableMilkBucket(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir){
        user.clearStatusEffects();
        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        if(user instanceof PlayerEntity player && !player.getInventory().insertStack(emptyBucket))
            player.dropItem(emptyBucket, false);
        cir.setReturnValue(stack);
    }
}