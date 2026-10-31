package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.mushrooms.StagedMushrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A small brown or red mushroom grows by stages (StagedMushrooms) instead of spreading as vanilla's do. */
@Mixin(MushroomBlock.class)
public abstract class MushroomBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void playersVersus$growByStages(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo info) {
        if (!VersusSettings.Mushrooms.DO_STAGED_MUSHROOMS || StagedMushrooms.kindOf(state) == null) return;
        StagedMushrooms.randomTick(state, level, pos, random);
        info.cancel();
    }
}
