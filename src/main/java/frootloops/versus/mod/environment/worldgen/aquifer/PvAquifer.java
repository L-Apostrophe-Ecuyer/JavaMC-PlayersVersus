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

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BAND_REACH;
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
 * them whole moved those steps (commit 373b78f). Their smooth inputs come from per-chunk {@link Lattice}s on the
 * terrain pass's cell grid (4 x 8 x 4, aligned like vanilla's cells): depth, cave entrances and S's inner part, which
 * the JSON router interpolated on those cells in the terrain pass; and 3D continentalness, which it sampled per block
 * but is smooth. The ridge noise is kept per column; the surface and ramen noises are sampled per block. Every caller
 * (the terrain pass, carvers, probes) gets the same answer at a block; {@code AquiferTerrainPassTest} compares it
 * with the terrain pass before.
 *
 * <p>Walls look at a block's neighbours ({@link PvAquiferRules#decide}), up to
 * {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#BAND_REACH} blocks away, so each block's own
 * decision ({@link PvAquiferRules#atPosition}) is kept once computed, for the chunk and that far around it. The
 * lattices and the ridge cache reach past the chunk too, and give there what the neighbouring chunk gives, so both
 * chunks agree on the walls along their border.
 */
public final class PvAquifer implements AquiferSampler {

    /**
     * The vertical size of the terrain pass's cells ({@code size_vertical} 2 in the noise settings, times 4), which
     * vanilla interpolates {@code interpolated} functions over. {@code WorldgenDataTest} checks the settings.
     */
    public static final int CELL_HEIGHT = 8;

    /** How far past the chunk the walls look, and so the columns kept below. */
    private static final int REACH = BAND_REACH;
    private static final int SIDE = 16 + 2 * REACH;
    /** The heights where {@link PvAquiferRules#atPosition} can say anything but air: the sea band, y -31..63. */
    private static final int MIN_Y = SEA_BAND_MIN_Y + 1;
    private static final int LEVELS = SEA_LEVEL - MIN_Y;
    private static final PvAquiferDecision[] DECISIONS = PvAquiferDecision.values();

    private final FluidLevelSampler fluidLevelSampler;
    private final AquiferFloodedness floodednessInputs;
    private final int originX, originZ;
    private final Lattice depth, continentalness, entrances, basinInner;
    /**
     * Ridge noise by column (it doesn't depend on y), {@code NaN} until sampled; made when first needed, since
     * heightmap probes make an aquifer for every column they sample and often never reach the sea band.
     */
    @Nullable
    private double[] ridge;
    /**
     * Each block's own decision, by column and then height from {@link #MIN_Y}: its ordinal plus one, 0 until
     * computed. Made when first needed, and each column's array too.
     */
    @Nullable
    private byte[][] positions;
    /** Where {@link #atPosition} samples the noises; one per aquifer, which only one thread uses at a time. */
    private final Position position = new Position();
    private final ToDoubleFunction<DensityFunction.NoisePos> floodedness = this::floodedness;
    private final ToDoubleFunction<DensityFunction.NoisePos> spread = this::spread;
    private final PvAquiferRules.Positions atPosition = this::atPosition;
    /** Blocks whose own decision was computed, for tests and the benchmark. */
    private int computedPositions;

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
        this.depth = new Lattice(depth, chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL, CELL_HEIGHT);
        this.continentalness = new Lattice(this.floodednessInputs.continentalness(), chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL, CELL_HEIGHT);
        Lattice entrances = new Lattice(this.floodednessInputs.entrances(), chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL, CELL_HEIGHT);
        this.entrances = entrances;
        AquiferSpread spread = inputs.spread();
        this.basinInner = new Lattice((x, y, z) -> {
            DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
            return AquiferFormulas.basinInner(y, entrances.exactAt(x, y, z), spread.noodle().sample(pos), spread.surface().sample(pos));
        }, chunkPos, BASIN_MIN_Y, BASIN_MAX_Y, CELL_HEIGHT);
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
        return PvAquiferRules.decide(pos, density, lavaLevel, this.atPosition);
    }

    /** {@link PvAquiferRules#atPosition} at a block, computed once for the blocks the walls look at. */
    public PvAquiferDecision atPosition(int x, int y, int z) {
        if (y < MIN_Y || y >= SEA_LEVEL) return PvAquiferDecision.AIR;
        int localX = x - this.originX + REACH, localZ = z - this.originZ + REACH;
        if (localX < 0 || localX >= SIDE || localZ < 0 || localZ >= SIDE) {
            return PvAquiferRules.atPosition(this.position.set(x, y, z), this.floodedness, this.spread);
        }
        int column = localX * SIDE + localZ;
        if (this.positions == null) this.positions = new byte[SIDE * SIDE][];
        byte[] levels = this.positions[column];
        if (levels == null) {
            levels = new byte[LEVELS];
            this.positions[column] = levels;
        }
        int level = y - MIN_Y;
        int stored = levels[level];
        if (stored != 0) return DECISIONS[stored - 1];
        PvAquiferDecision decision = PvAquiferRules.atPosition(this.position.set(x, y, z), this.floodedness, this.spread);
        levels[level] = (byte) (decision.ordinal() + 1);
        this.computedPositions++;
        return decision;
    }

    @Override
    public boolean needsFluidTick() {
        return this.needsFluidTick;
    }

    /** F at a block, from lattice inputs; {@code /pvwg probe} shows it next to the exact value. */
    public double floodedness(DensityFunction.NoisePos pos) {
        int x = pos.blockX(), y = pos.blockY(), z = pos.blockZ();
        double entrances = this.entrances.at(x, y, z);
        double seaFloodedness = AquiferFormulas.seaFloodedness(y, this.depth.at(x, y, z), this.continentalness.at(x, y, z),
                entrances, entrances, this.ridge(x, z), this.floodednessInputs.surface().sample(pos));
        return AquiferFormulas.floodedness(y, seaFloodedness, this.floodednessInputs.ramen(), pos);
    }

    /** S at a block, from the lattice of its inner part. */
    public double spread(DensityFunction.NoisePos pos) {
        int y = pos.blockY();
        return AquiferFormulas.spread(y, this.basinInner.at(pos.blockX(), y, pos.blockZ()));
    }

    private double ridge(int x, int z) {
        int localX = x - this.originX + REACH, localZ = z - this.originZ + REACH;
        if (localX < 0 || localX >= SIDE || localZ < 0 || localZ >= SIDE) {
            return this.floodednessInputs.ridge().sample(new DensityFunction.UnblendedNoisePos(x, 0, z));
        }
        if (this.ridge == null) {
            this.ridge = new double[SIDE * SIDE];
            Arrays.fill(this.ridge, Double.NaN);
        }
        int index = localX * SIDE + localZ;
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

    /** Blocks whose own decision was computed so far (each once), for tests and the benchmark. */
    public int computedPositions() {
        return this.computedPositions;
    }

    /** A block position to sample the noises at, set for each block {@link #atPosition} computes. */
    private static final class Position implements DensityFunction.NoisePos {
        private int x, y, z;

        Position set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public int blockX() {
            return this.x;
        }

        @Override
        public int blockY() {
            return this.y;
        }

        @Override
        public int blockZ() {
            return this.z;
        }
    }
}
