package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.jetbrains.annotations.Nullable;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BAND_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;

/**
 * The Players Versus aquifer: sea-level water for oceans and rivers, barriers that keep it out of caves, water basins
 * in low caves, dry caves everywhere else, lava at the bottom. The rules live in {@link PvAquiferRules}.
 *
 * <p>One instance exists per {@link net.minecraft.world.gen.chunk.ChunkNoiseSampler}, created by
 * {@code ChunkNoiseSamplerMixin} for Players Versus generators only. It answers the terrain pass, the carvers (which
 * pass {@code density = 0}) and heightmap probes, all from the same two {@link Lattice}s, so they agree block for
 * block, and so do neighbouring chunks.
 */
public final class PvAquifer implements AquiferSampler {

    private final FluidLevelSampler fluidLevelSampler;
    /** Sea/river floodedness, only read strictly between {@code SEA_BAND_MIN_Y} and {@code SEA_LEVEL}. */
    private final Lattice floodedness;
    /** Cave-basin floodedness, only read strictly between {@code BASIN_MIN_Y} and {@code BASIN_MAX_Y}. */
    private final Lattice spread;

    /**
     * Only read right after {@link #apply} returned a fluid, and every fluid decision sets it, so it never leaks a
     * stale value between positions.
     */
    private boolean needsFluidTick;

    public PvAquifer(NoiseRouter noiseRouter, ChunkPos chunkPos, FluidLevelSampler fluidLevelSampler) {
        this.fluidLevelSampler = fluidLevelSampler;
        this.floodedness = new Lattice(noiseRouter.fluidLevelFloodednessNoise(), chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL);
        this.spread = new Lattice(noiseRouter.fluidLevelSpreadNoise(), chunkPos, BASIN_MIN_Y, BASIN_MAX_Y);
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

    /** Lattice points sampled so far (floodedness, spread), for tests and the benchmark. */
    public int[] latticeSamples() {
        return new int[]{this.floodedness.samples(), this.spread.samples()};
    }
}
