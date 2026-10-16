package frootloops.versus.mixin.environment.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ScaffoldingBlockItem.class)
public class ScaffoldingItemMixin {

    @ModifyVariable(method = "updatePlacementContext", at = @At("STORE"), ordinal = 0)
    private Direction placeWhereverYouLook(Direction originalDirection, BlockPlaceContext context) {
        float pitch = context.getPlayer().getXRot();
        if(Math.abs(pitch) > 60.0f) {
            if(context.getClickedFace() == Direction.UP || pitch > 80f) return Direction.UP;
            if(context.getClickedFace() == Direction.DOWN || pitch < -80f) return Direction.UP;
        }
        else if(context.isSecondaryUseActive() || context.isInside()) {
            return context.getHorizontalDirection();
        }
        return originalDirection;
    }
}
