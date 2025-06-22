
package frootloops.versus.mixin.items.brewing;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin
        extends LockableContainerBlockEntity
        implements SidedInventory {

    protected BrewingStandBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private static void setFuel(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo info) {
        ItemStack fuelStack = blockEntity.getStack(4);
        int fuel = ((BrewingStandBlockEntityAccessor)blockEntity).getFuel();
        int brewTime = ((BrewingStandBlockEntityAccessor)blockEntity).getBrewTime();

        if (brewTime % 20 == 10 && fuel > 0) {
            ((BrewingStandBlockEntityAccessor)blockEntity).setFuel(fuel - 1);
            fuel--;
        }

        if (fuel <= 0 && fuelStack.isOf(Items.NETHER_WART)) {
            ((BrewingStandBlockEntityAccessor)blockEntity).setFuel(21);
            fuelStack.decrement(1);
            BrewingStandBlockEntity.markDirty(world, pos, state);
        }
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == 3) {
            BrewingRecipeRegistry brewingRecipeRegistry = this.world != null ? this.world.getBrewingRecipeRegistry() : BrewingRecipeRegistry.EMPTY;
            return brewingRecipeRegistry.isValidIngredient(stack);
        }
        if (slot == 4) {
            return stack.isOf(CustomBlockItems.CORRUPTED_WART) || stack.isOf(CustomBlockItems.WITHERED_WART) || stack.isOf(Items.NETHER_WART);
        }
        return (stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) && this.getStack(slot).isEmpty();
    }
}

