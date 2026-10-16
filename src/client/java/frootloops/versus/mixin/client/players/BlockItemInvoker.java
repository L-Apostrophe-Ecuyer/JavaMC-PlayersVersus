package frootloops.versus.mixin.client.players;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** What a block item would place, for the placement preview (ClientPlacement). */
@Mixin(BlockItem.class)
public interface BlockItemInvoker {
    @Nullable
    @Invoker("getPlacementState")
    BlockState playersVersus$getPlacementState(BlockPlaceContext context);
}
