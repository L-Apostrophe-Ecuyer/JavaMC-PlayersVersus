package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

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
 * pass {@code density = 0}) and heightmap probes.
 *
 * <p>F and S ({@link AquiferFormulas}) are evaluated per block, because they step inside their bands; interpolating
 * them whole moved those steps (commit 373b78f). Their smooth inputs come from per-chunk {@link Lattice}s instead of
 * per-block noise: depth, 3D continentalness, cave entrances, and S's inner part, which the JSON interpolated anyway.
 * The ridge noise is kept per column, the surface and ramen noises are sampled per block.
 */
public final class PvAquifer implements AquiferSampler {

    private final FluidLevelSampler fluidLevelSampler;
    private final AquiferFloodedness floodednessInputs;
    private final int originX, originZ;
    private final Lattice depth, continentalness, entrances, basinInner;
    /** Ridge noise by column (it doesn't depend on y), {@code NaN} until sampled. */
    private final double[] ridge = new double[16 * 16];
    private final ToDoubleFunction<DensityFunction.NoisePos> floodedness = this::floodedness;
    private final ToDoubleFunction<DensityFunction.NoisePos> spread = this::spread;

    /**
     * Only read right after {@link #apply} returned a fluid, and every fluid decision sets it, so it never leaks a
     * stale value between positions.
     */
    private boolean needsFluidTick;

    /**
     * @param depth the chunk's own router's {@code depth}: sampled at lattice points, where its 2D parts come from the
     *              chunk's cache
     */
    public PvAquifer(AquiferInputs inputs, DensityFunction depth, ChunkPos chunkPos, FluidLevelSampler fluidLevelSampler) {
        this.fluidLevelSampler = fluidLevelSampler;
        this.floodednessInputs = inputs.floodedness();
        this.originX = chunkPos.getStartX();
        this.originZ = chunkPos.getStartZ();
        this.depth = new Lattice(depth, chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL);
        this.continentalness = new Lattice(this.floodednessInputs.continentalness(), chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL);
        Lattice entrances = new Lattice(this.floodednessInputs.entrances(), chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL);
        this.entrances = entrances;
        AquiferSpread spread = inputs.spread();
        this.basinInner = new Lattice((x, y, z) -> {
            DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
            return AquiferFormulas.basinInner(y, entrances.exactAt(x, y, z), spread.noodle().sample(pos), spread.surface().sample(pos));
        }, chunkPos, BASIN_MIN_Y, BASIN_MAX_Y);
        Arrays.fill(this.ridge, Double.NaN);
    }

    @Override
    @Nullable
    public BlockState apply(DensityFunction.NoisePos pos, double density) {
        int y = pos.blockY();
        boolean lavaLevel = density <= 0.0
                && this.fluidLevelSampler.getFluidLevel(pos.blockX(), y, pos.blockZ()).getBlockState(y).isOf(Blocks.LAVA);
        PvAquiferDecision decision = this.decide(pos, density, lavaLevel);
        this.needsFluidTick = decision.needsFluidTick;
        return decision.state;
    }

    /** What the aquifer places at {@code pos}; {@code /pvwg probe} asks this too. */
    public PvAquiferDecision decide(DensityFunction.NoisePos pos, double density, boolean lavaLevel) {
        return PvAquiferRules.decide(pos, density, lavaLevel, this.floodedness, this.spread);
    }

    @Override
    public boolean needsFluidTick() {
        return this.needsFluidTick;
    }

    /** F at a block, from lattice inputs; {@code /pvwg probe} shows it next to the exact value. */
    public double floodedness(DensityFunction.NoisePos pos) {
        int x = pos.blockX(), y = pos.blockY(), z = pos.blockZ();
        double seaFloodedness = AquiferFormulas.seaFloodedness(y, this.depth.at(x, y, z), this.continentalness.at(x, y, z),
                this.entrances.at(x, y, z), this.ridge(x, z), this.floodednessInputs.surface().sample(pos));
        return AquiferFormulas.floodedness(y, seaFloodedness, () -> this.floodednessInputs.ramen().sample(pos));
    }

    /** S at a block, from the lattice of its inner part. */
    public double spread(DensityFunction.NoisePos pos) {
        int y = pos.blockY();
        return AquiferFormulas.spread(y, this.basinInner.at(pos.blockX(), y, pos.blockZ()));
    }

    private double ridge(int x, int z) {
        int localX = x - this.originX, localZ = z - this.originZ;
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            return this.floodednessInputs.ridge().sample(new DensityFunction.UnblendedNoisePos(x, 0, z));
        }
        int index = localX * 16 + localZ;
        double value = this.ridge[index];
        if (Double.isNaN(value)) {
            value = this.floodednessInputs.ridge().sample(new DensityFunction.UnblendedNoisePos(x, 0, z));
            this.ridge[index] = value;
        }
        return value;
    }

    /** Lattice points sampled so far (depth, continentalness, entrances, basin inner), for tests and the benchmark. */
    public int[] latticeSamples() {
        return new int[]{this.depth.samples(), this.continentalness.samples(), this.entrances.samples(), this.basinInner.samples()};
    }
}
