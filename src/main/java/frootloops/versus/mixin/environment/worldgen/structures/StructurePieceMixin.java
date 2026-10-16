package frootloops.versus.mixin.environment.worldgen.structures;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {

    @ModifyVariable(method = "maybeGenerateBlock", at = @At("HEAD"), ordinal = 0)
    private BlockState extinguished(BlockState state) {
        if(state.is(Blocks.WALL_TORCH)) state = CustomBlocks.EXTINGUISHED_WALL_TORCH.withPropertiesOf(state);
        return state;
    }
}
