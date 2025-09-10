package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.VanillaItemsAndStacks;
import frootloops.versus.mod.items.VanillaItemsV2;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.component.ComponentHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ItemStack.class)
public abstract class VanillaItemsMixin implements ComponentHolder, FabricItemStack {

        @Shadow private final Item item;

        protected VanillaItemsMixin(Item item) {
                this.item = item;
        }

        @ModifyVariable(method = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/ItemConvertible;I)V", at = @At("HEAD"), ordinal = 0)
        private static ItemConvertible injected(ItemConvertible item) {
                return (ItemConvertible) VanillaItemsV2.getReplacementItem(item.asItem());
        }

        @Inject(method = "isItemEnabled", at = @At("HEAD"), cancellable = true)
        public void isItemEnabled(FeatureSet enabledFeatures, CallbackInfoReturnable<Boolean> cir) {
                if(VanillaItemsV2.hasReplacementItem(item)) cir.setReturnValue(false);
        }

        @Inject(method = "onStackClicked", at = @At("TAIL"), cancellable = false)
        public void onStackClicked(Slot slot, ClickType clickType, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
                if(VanillaItemsV2.hasReplacementItem(item)) {
                        slot.setStack(((ItemStack)((Object)this)).withItem(VanillaItemsV2.getReplacementItem(item)));
                }
        }
}
