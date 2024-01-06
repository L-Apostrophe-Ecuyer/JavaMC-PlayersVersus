package frootloops.versus.mixin.environment.worldgen.structures;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {

    @ModifyVariable(method = "addBlockWithRandomThreshold", at = @At("HEAD"), ordinal = 8)
    private BlockState extinguished(BlockState state) {
        if(state.isOf(Blocks.WALL_TORCH)) state = CustomBlocks.EXTINGUISHED_WALL_TORCH.getStateWithProperties(state);
        return state;
    }
}
