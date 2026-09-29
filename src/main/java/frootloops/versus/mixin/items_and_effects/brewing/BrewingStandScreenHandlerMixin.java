package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandScreenHandlerMixin extends AbstractContainerMenu {

    @Shadow
    private Container brewingStand;
    @Shadow
    private ContainerData brewingStandData;

    protected BrewingStandScreenHandlerMixin(@Nullable MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Redirect(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BrewingStandMenu$FuelSlot;mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean quickMoveNetherWart(ItemStack stack) {
        return stack.is(Items.NETHER_WART);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if(this.slots.size() == 4) {
            return super.addSlot(new NetherWartFuelSlot(brewingStand, 4, 17, 17));
        }
        else return super.addSlot(slot);
    }

    public static class NetherWartFuelSlot extends Slot {
        public NetherWartFuelSlot(Container inventory, int i, int j, int k) {
            super(inventory, i, j, k);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return NetherWartFuelSlot.matches(itemStack);
        }

        public static boolean matches(ItemStack itemStack) {
            return itemStack.is(CustomBlockItems.CORRUPTED_WART) || itemStack.is(CustomBlockItems.WITHERED_WART) || itemStack.is(Items.NETHER_WART);
        }
    }
}

