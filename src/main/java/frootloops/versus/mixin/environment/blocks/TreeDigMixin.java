package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.trees.Felling;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cutting a tree takes longer the more of it would fall (Felling.digTimeFactor), whether it's cut through its trunk or
 * dug out from under it: the block a tree rests on breaks as slowly as the trunk above it would. Worked out alike on
 * the client and the server, from the blocks, so both break it on the same tick.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TreeDigMixin {

    @ModifyReturnValue(method = "getDestroyProgress", at = @At("RETURN"))
    private float playersVersus$slowUnderATree(float progress, Player player, BlockGetter level, BlockPos pos) {
        if (!VersusSettings.Trees.DO_FELLING || progress <= 0.0f || !(level instanceof LevelReader reader)) return progress;
        return (float) (progress / Felling.digTimeFactor(reader, pos, (BlockState) (Object) this));
    }
}
