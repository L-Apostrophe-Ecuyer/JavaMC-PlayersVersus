package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.jetbrains.annotations.Nullable;

/**
 * The Players Versus aquifer: sea-level water for oceans and rivers, stone barriers that keep it out of caves, water
 * basins in low caves, dry caves everywhere else, lava at the bottom. The rules live in {@link PvAquiferRules}.
 *
 * <p>One instance exists per {@link net.minecraft.world.gen.chunk.ChunkNoiseSampler}, created by
 * {@code ChunkNoiseSamplerMixin} for Players Versus generators only. The same instance answers the terrain pass,
 * the carvers (which pass {@code density = 0}) and heightmap probes.
 */
public final class PvAquifer implements AquiferSampler {

    private final FluidLevelSampler fluidLevelSampler;
    private final DensityFunction floodedness;
    private final DensityFunction spread;

    /**
     * Only read right after {@link #apply} returned a fluid, and every fluid decision sets it, so it never leaks a
     * stale value between positions.
     */
    private boolean needsFluidTick;

    public PvAquifer(NoiseRouter noiseRouter, FluidLevelSampler fluidLevelSampler) {
        this.fluidLevelSampler = fluidLevelSampler;
        this.floodedness = noiseRouter.fluidLevelFloodednessNoise();
        this.spread = noiseRouter.fluidLevelSpreadNoise();
    }

    @Override
    @Nullable
    public BlockState apply(DensityFunction.NoisePos pos, double density) {
        if (density > 0.0) {
            this.needsFluidTick = false;
            return null;
        }
        int y = pos.blockY();
        boolean lavaLevel = this.fluidLevelSampler.getFluidLevel(pos.blockX(), y, pos.blockZ()).getBlockState(y).isOf(Blocks.LAVA);
        PvAquiferDecision decision = PvAquiferRules.decide(pos, density, lavaLevel, this.floodedness, this.spread);
        this.needsFluidTick = decision.needsFluidTick;
        return decision.state;
    }

    @Override
    public boolean needsFluidTick() {
        return this.needsFluidTick;
    }
}
