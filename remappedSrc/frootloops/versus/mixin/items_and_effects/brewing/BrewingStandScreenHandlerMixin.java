package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewingStandScreenHandler.class)
public abstract class BrewingStandScreenHandlerMixin extends ScreenHandler {

    @Shadow
    private Inventory inventory;
    @Shadow
    private PropertyDelegate propertyDelegate;

    protected BrewingStandScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Redirect(method = "quickMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/BrewingStandScreenHandler$FuelSlot;matches(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean quickMoveNetherWart(ItemStack stack) {
        return stack.isOf(Items.NETHER_WART);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if(this.slots.size() == 4) {
            return super.addSlot(new NetherWartFuelSlot(inventory, 4, 17, 17));
        }
        else return super.addSlot(slot);
    }

    public static class NetherWartFuelSlot extends Slot {
        public NetherWartFuelSlot(Inventory inventory, int i, int j, int k) {
            super(inventory, i, j, k);
        }

        @Override
        public boolean canInsert(ItemStack itemStack) {
            return NetherWartFuelSlot.matches(itemStack);
        }

        public static boolean matches(ItemStack itemStack) {
            return itemStack.isOf(CustomBlockItems.CORRUPTED_WART) || itemStack.isOf(CustomBlockItems.WITHERED_WART) || itemStack.isOf(Items.NETHER_WART);
        }
    }
}

