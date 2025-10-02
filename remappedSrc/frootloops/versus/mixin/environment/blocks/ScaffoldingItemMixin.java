package frootloops.versus.mixin.environment.blocks;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ScaffoldingItem;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ScaffoldingItem.class)
public class ScaffoldingItemMixin {

    @ModifyVariable(method = "getPlacementContext", at = @At("STORE"), ordinal = 0)
    private Direction placeWhereverYouLook(Direction originalDirection, ItemPlacementContext context) {
        float pitch = context.getPlayer().getPitch();
        if(Math.abs(pitch) > 60.0f) {
            if(context.getSide() == Direction.UP || pitch > 80f) return Direction.UP;
            if(context.getSide() == Direction.DOWN || pitch < -80f) return Direction.UP;
        }
        else if(context.shouldCancelInteraction() || context.hitsInsideBlock()) {
            return context.getHorizontalPlayerFacing();
        }
        return originalDirection;
    }
}
