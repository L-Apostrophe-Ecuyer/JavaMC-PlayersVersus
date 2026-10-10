package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquifer;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The aquifer hook of the Players Versus world type: swaps in {@link PvAquifer} where a chunk's {@link NoiseChunk} makes
 * its aquifer from the noise settings' aquifer config, when that config names the Players Versus aquifer's inputs
 * ({@link PvAquifer#create}). Every other generator, including all vanilla world types, gets vanilla's aquifer. (The
 * ore veins, which this hook also swapped before 26.3, are material rules in the Players Versus settings now.)
 *
 * <p>{@code @WrapOperation} chains with other mods that wrap the same call, unlike {@code @Redirect}/{@code @Overwrite}.
 */
@Mixin(NoiseChunk.class)
public abstract class ChunkNoiseSamplerMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/Aquifer$Config;create(Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;Lnet/minecraft/world/level/levelgen/densityfunction/DensityVolume;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;"
            )
    )
    private Aquifer playersVersus$useAquifer(Aquifer.Config config, DensitySamplerSet samplers, PositionalRandomFactory random,
                                             DensityVolume volume, Aquifer.FluidPicker fluidPicker, Operation<Aquifer> original) {
        PvAquifer aquifer = PvAquifer.create(config, samplers, volume, fluidPicker);
        return aquifer != null ? aquifer : original.call(config, samplers, random, volume, fluidPicker);
    }
}
