package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferInputs;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquifer;
import frootloops.versus.mod.environment.worldgen.ore.PvOreVeins;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The only worldgen hook of the Players Versus world type: swaps in {@link PvAquifer} and {@link PvOreVeins} when the
 * chunk generator's settings are Players Versus settings ({@link PvWorldgen#isPvGenerator}). Every other generator,
 * including all vanilla world types, gets vanilla's aquifer and ore veins.
 *
 * <p>{@code @WrapOperation} chains with other mods that wrap the same calls, unlike {@code @Redirect}/{@code @Overwrite}.
 */
@Mixin(NoiseChunk.class)
public abstract class ChunkNoiseSamplerMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/Aquifer;create(Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/NoiseRouter;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;IILnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;"
            )
    )
    private Aquifer playersVersus$useAquifer(NoiseChunk chunkNoiseSampler, ChunkPos chunkPos, NoiseRouter noiseRouter,
                                                   PositionalRandomFactory randomSplitter, int minimumY, int height,
                                                   Aquifer.FluidPicker fluidLevelSampler,
                                                   Operation<Aquifer> original,
                                                   @Local(argsOnly = true) NoiseGeneratorSettings settings,
                                                   @Local(argsOnly = true) RandomState noiseConfig) {
        if (PvWorldgen.isPvGenerator(settings)) {
            return new PvAquifer(AquiferInputs.of(noiseConfig, settings), noiseRouter.depth(), chunkPos, fluidLevelSampler);
        }
        return original.call(chunkNoiseSampler, chunkPos, noiseRouter, randomSplitter, minimumY, height, fluidLevelSampler);
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/OreVeinifier;create(Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/DensityFunction;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)Lnet/minecraft/world/level/levelgen/NoiseChunk$BlockStateFiller;"
            )
    )
    private NoiseChunk.BlockStateFiller playersVersus$useOreVeins(DensityFunction veinToggle, DensityFunction veinRidged,
                                                                         DensityFunction veinGap, PositionalRandomFactory randomDeriver,
                                                                         Operation<NoiseChunk.BlockStateFiller> original,
                                                                         @Local(argsOnly = true) NoiseGeneratorSettings settings) {
        if (PvWorldgen.isPvGenerator(settings)) {
            return PvOreVeins.create(veinToggle, veinRidged, veinGap, randomDeriver);
        }
        return original.call(veinToggle, veinRidged, veinGap, randomDeriver);
    }
}
