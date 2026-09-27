package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquifer;
import frootloops.versus.mod.environment.worldgen.ore.PvOreVeins;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The only worldgen hook of the Players Versus world type: swaps in {@link PvAquifer} and {@link PvOreVeins} when the
 * chunk generator's settings are Players Versus settings ({@link PvWorldgen#isPvGenerator}). Every other generator,
 * including all vanilla world types, gets vanilla's aquifer and ore veins.
 *
 * <p>{@code @WrapOperation} chains with other mods that wrap the same calls, unlike {@code @Redirect}/{@code @Overwrite}.
 */
@Mixin(ChunkNoiseSampler.class)
public abstract class ChunkNoiseSamplerMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/chunk/AquiferSampler;aquifer(Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/gen/noise/NoiseRouter;Lnet/minecraft/util/math/random/RandomSplitter;IILnet/minecraft/world/gen/chunk/AquiferSampler$FluidLevelSampler;)Lnet/minecraft/world/gen/chunk/AquiferSampler;"
            )
    )
    private AquiferSampler playersVersus$useAquifer(ChunkNoiseSampler chunkNoiseSampler, ChunkPos chunkPos, NoiseRouter noiseRouter,
                                                   RandomSplitter randomSplitter, int minimumY, int height,
                                                   AquiferSampler.FluidLevelSampler fluidLevelSampler,
                                                   Operation<AquiferSampler> original,
                                                   @Local(argsOnly = true) ChunkGeneratorSettings settings) {
        if (PvWorldgen.isPvGenerator(settings)) {
            return new PvAquifer(noiseRouter, chunkPos, fluidLevelSampler);
        }
        return original.call(chunkNoiseSampler, chunkPos, noiseRouter, randomSplitter, minimumY, height, fluidLevelSampler);
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/OreVeinSampler;create(Lnet/minecraft/world/gen/densityfunction/DensityFunction;Lnet/minecraft/world/gen/densityfunction/DensityFunction;Lnet/minecraft/world/gen/densityfunction/DensityFunction;Lnet/minecraft/util/math/random/RandomSplitter;)Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler$BlockStateSampler;"
            )
    )
    private ChunkNoiseSampler.BlockStateSampler playersVersus$useOreVeins(DensityFunction veinToggle, DensityFunction veinRidged,
                                                                         DensityFunction veinGap, RandomSplitter randomDeriver,
                                                                         Operation<ChunkNoiseSampler.BlockStateSampler> original,
                                                                         @Local(argsOnly = true) ChunkGeneratorSettings settings) {
        if (PvWorldgen.isPvGenerator(settings)) {
            return PvOreVeins.create(veinToggle, veinRidged, veinGap, randomDeriver);
        }
        return original.call(veinToggle, veinRidged, veinGap, randomDeriver);
    }
}
