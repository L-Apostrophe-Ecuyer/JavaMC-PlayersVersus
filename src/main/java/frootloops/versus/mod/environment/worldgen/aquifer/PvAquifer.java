package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToDoubleFunction;

/**
 * The Players Versus aquifer: sea-level water for oceans and rivers, barriers that keep it out of caves, water basins
 * in low caves, dry caves everywhere else, lava at the bottom. The rules live in {@link PvAquiferRules}.
 *
 * <p>One instance exists per {@link net.minecraft.world.gen.chunk.ChunkNoiseSampler}, created by
 * {@code ChunkNoiseSamplerMixin} for Players Versus generators only. It answers the terrain pass, the carvers (which
 * pass {@code density = 0}) and heightmap probes.
 *
 * <p>It samples floodedness and spread per block. Interpolating them whole on a {@link Lattice} was tried and measured
 * (commit 373b78f, {@code AquiferLatticeTest}): both functions step inside their bands (floodedness is 0 from y 64
 * up, the ramen term only applies below y 32 and inside the barrier band, spread's factor jumps at y 24), so
 * interpolating across those steps changed 19% of decisions in y 48..63 and put barriers on ocean surfaces. A lattice
 * may only hold their smooth inputs; see the plan, Section 6.2.
 */
public final class PvAquifer implements AquiferSampler {

    private final FluidLevelSampler fluidLevelSampler;
    private final ToDoubleFunction<DensityFunction.NoisePos> floodedness;
    private final ToDoubleFunction<DensityFunction.NoisePos> spread;

    /**
     * Only read right after {@link #apply} returned a fluid, and every fluid decision sets it, so it never leaks a
     * stale value between positions.
     */
    private boolean needsFluidTick;

    public PvAquifer(NoiseRouter noiseRouter, ChunkPos chunkPos, FluidLevelSampler fluidLevelSampler) {
        this.fluidLevelSampler = fluidLevelSampler;
        this.floodedness = noiseRouter.fluidLevelFloodednessNoise()::sample;
        this.spread = noiseRouter.fluidLevelSpreadNoise()::sample;
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
