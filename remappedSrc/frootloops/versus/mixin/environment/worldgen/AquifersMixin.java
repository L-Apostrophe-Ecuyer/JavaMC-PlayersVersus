package frootloops.versus.mixin.environment.worldgen;


import frootloops.versus.mod.environment.worldgen.SimpleWaterAquifer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(ChunkNoiseSampler.class)
public abstract class AquifersMixin {

    @Redirect(method = "<init>", at =
        @At(
                value = "INVOKE", target =
                "Lnet/minecraft/world/gen/chunk/AquiferSampler;aquifer(Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/gen/noise/NoiseRouter;Lnet/minecraft/util/math/random/RandomSplitter;IILnet/minecraft/world/gen/chunk/AquiferSampler$FluidLevelSampler;)Lnet/minecraft/world/gen/chunk/AquiferSampler;"
        )
    )
    public AquiferSampler aquifer(ChunkNoiseSampler chunkNoiseSampler, ChunkPos chunkPos, NoiseRouter noiseRouter, RandomSplitter randomSplitter, int minimumY, int height, AquiferSampler.FluidLevelSampler fluidLevelSampler) {
        return new SimpleWaterAquifer(chunkNoiseSampler, chunkPos, noiseRouter, randomSplitter, minimumY, height, fluidLevelSampler);
    }
}
