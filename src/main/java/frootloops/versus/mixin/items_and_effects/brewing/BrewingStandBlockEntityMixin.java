
package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin
        extends BaseContainerBlockEntity
        implements WorldlyContainer {

    protected BrewingStandBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "serverTick", at = @At(value = "HEAD"))
    private static void setFuel(ServerLevel world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo info) {
        ItemStack fuelStack = blockEntity.getItem(4);
        int fuel = ((BrewingStandBlockEntityAccessor)blockEntity).getFuel();
        int brewTime = ((BrewingStandBlockEntityAccessor)blockEntity).getBrewTime();

        if (brewTime % 20 == 10 && fuel > 0) {
            ((BrewingStandBlockEntityAccessor)blockEntity).setFuel(fuel - 1);
            fuel--;
        }

        if (fuel <= 0 && fuelStack.is(Items.NETHER_WART)) {
            ((BrewingStandBlockEntityAccessor)blockEntity).setFuel(21);
            fuelStack.shrink(1);
            BrewingStandBlockEntity.setChanged(world, pos, state);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 3) {
            // Brewing is recipes since 26.3: the reagents are the brewing recipes' property set, as vanilla checks it.
            return this.level != null && this.level.recipeAccess().propertySet(RecipePropertySet.BREWING_REAGENTS).test(stack);
        }
        if (slot == 4) {
            return stack.is(CustomBlockItems.CORRUPTED_WART) || stack.is(CustomBlockItems.WITHERED_WART) || stack.is(Items.NETHER_WART);
        }
        return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) && this.getItem(slot).isEmpty();
    }
}

