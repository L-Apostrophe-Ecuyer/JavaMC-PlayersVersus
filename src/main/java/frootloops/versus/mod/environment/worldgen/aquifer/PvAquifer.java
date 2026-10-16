package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BAND_REACH;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_UPPER_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_UPPER_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BAND_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;

/**
 * The Players Versus aquifer: sea-level water for oceans and rivers, barriers that keep it out of caves, water basins
 * in low caves, dry caves everywhere else, lava at the bottom, and the high river's water at y 80 and in its upper
 * layer at y 96. The rules live in {@link PvAquiferRules}.
 *
 * <p>One instance exists per {@link net.minecraft.world.level.levelgen.NoiseChunk}, created by
 * {@code ChunkNoiseSamplerMixin} for generators whose aquifer config names {@link AquiferFloodedness} ({@link #create}).
 * It answers the terrain pass, the carved blocks (which 26.3 passes with {@code density = 0}) and heightmap probes.
 *
 * <p>Its inputs are the functions the config names, compiled by the world and bound to the chunk's sampling context,
 * so they share its caches ({@link DensitySamplerSet}). F and S ({@link AquiferFormulas}) are evaluated per block,
 * because they step inside their bands; their smooth inputs come from per-chunk {@link Lattice}s on the terrain's cells
 * (4 x 8 x 4): depth, cave entrances and S's inner part, which 1.21.10's JSON interpolated on those cells, and 3D
 * continentalness, which it sampled per block but is smooth. The ridge noise is kept per column; the surface and ramen
 * noises are sampled per block.
 *
 * <p>The flooded corridors and the high river are water exactly where the final density opens them: the aquifer reads
 * the functions the final density takes the minimum with ({@code high_river/valley} and {@code high_river/upper_valley},
 * and {@code caves/flooded_corridors}, which is {@code caves/corridor_noodle} inside the corridors' zone), over the
 * chunk and {@code BAND_REACH} blocks around it, with one volume sample each ({@link Region}), which is how the terrain
 * pass samples them, so both get the same floats at every block.
 *
 * <p>Walls look at a block's neighbours ({@link PvAquiferRules#decide}), up to {@code BAND_REACH} blocks away, so each
 * block's own decision ({@link PvAquiferRules#atPosition}) is kept once computed, for the chunk and that far around it.
 * The lattices and the ridge cache reach past the chunk too, and give there what the neighbouring chunk gives, so both
 * chunks agree on the walls along their border.
 *
 * <p>Everything is made when first needed: heightmap probes make an aquifer for every column they sample and often
 * never reach the sea band.
 */
public final class PvAquifer implements Aquifer {

    /**
     * The terrain's cell height (its {@code interpolated} functions' {@code cell_size_y}), which 1.21.10 interpolated
     * the aquifer's smooth inputs over.
     */
    public static final int CELL_HEIGHT = 8;

    /** How far past the chunk the walls look, and so the columns kept below. */
    private static final int REACH = BAND_REACH;
    private static final int SIDE = 16 + 2 * REACH;
    /** The heights where {@link PvAquiferRules#atPosition} can say anything but air: the sea band, y -31..63. */
    private static final int MIN_Y = SEA_BAND_MIN_Y + 1;
    private static final int LEVELS = SEA_LEVEL - MIN_Y;
    private static final PvAquiferDecision[] DECISIONS = PvAquiferDecision.values();

    private final FluidPicker fluidPicker;
    private final DensitySamplerSet samplers;
    /** The functions the config names: F's and S's inputs, the corridors' noodle and the high river's valleys. */
    private final AquiferFloodedness floodednessConfig;
    private final AquiferSpread spreadConfig;
    /** The chunk's first block, which the lattices and the kept decisions are laid out from. */
    private final int originX, originZ;
    /** The volume the aquifer answers for, and {@code REACH} around it: where the corridors and the valley are sampled. */
    private final int regionMinX, regionMinZ, regionMaxX, regionMaxZ;

    @Nullable
    private FloodednessInputs floodedness;
    @Nullable
    private SpreadInputs spread;
    @Nullable
    private Region corridors, dryPaths, valley, upperValley;
    /** Ridge noise by column (it doesn't depend on y), {@code NaN} until sampled. */
    @Nullable
    private double[] ridge;
    /** The basins' level, by column. */
    @Nullable
    private Columns basinLevel;
    /** Each block's own decision, by column and then height from {@link #MIN_Y}: its ordinal plus one, 0 until computed. */
    @Nullable
    private byte[][] positions;
    private final PvAquiferRules.Field floodednessField = this::floodedness;
    private final PvAquiferRules.Field spreadField = this::spread;
    private final PvAquiferRules.Field corridorField = this::corridor;
    private final PvAquiferRules.Field dryPathField = this::dryPath;
    private final PvAquiferRules.Field levelField = (x, y, z) -> this.basinLevel(x, z);
    private final PvAquiferRules.Positions atPosition = this::atPosition;
    /** Blocks whose own decision was computed, for tests and the benchmark. */
    private int computedPositions;

    /**
     * Only read right after {@link #computeSubstance} returned a fluid, and every fluid decision sets it, so it never leaks a
     * stale value between positions.
     */
    private boolean needsFluidTick;

    private PvAquifer(AquiferFloodedness floodednessConfig, AquiferSpread spreadConfig, DensitySamplerSet samplers, DensityVolume volume,
                      FluidPicker fluidPicker) {
        this.fluidPicker = fluidPicker;
        this.samplers = samplers;
        this.floodednessConfig = floodednessConfig;
        this.spreadConfig = spreadConfig;
        this.originX = Math.floorDiv(volume.minBlockX(), 16) * 16;
        this.originZ = Math.floorDiv(volume.minBlockZ(), 16) * 16;
        this.regionMinX = volume.minBlockX() - REACH;
        this.regionMinZ = volume.minBlockZ() - REACH;
        this.regionMaxX = volume.maxBlockX() + REACH;
        this.regionMaxZ = volume.maxBlockZ() + REACH;
    }

    /**
     * The Players Versus aquifer for a chunk's volume, if the aquifer config names its inputs
     * ({@link AquiferFloodedness} for the floodedness, {@link AquiferSpread} for the spread); {@code null} for any
     * other config, which keeps vanilla's aquifer.
     */
    @Nullable
    public static PvAquifer create(Aquifer.Config config, DensitySamplerSet samplers, DensityVolume volume, FluidPicker fluidPicker) {
        if (!(PvWorldgen.unwrap(config.fluidLevelFloodednessNoise()) instanceof AquiferFloodedness floodedness)) return null;
        if (!(PvWorldgen.unwrap(config.fluidLevelSpreadNoise()) instanceof AquiferSpread spread)) {
            throw new IllegalStateException("A Players Versus aquifer config needs players-versus:aquifer_spread in fluid_level_spread,"
                    + " found " + config.fluidLevelSpreadNoise());
        }
        return new PvAquifer(floodedness, spread, samplers, volume, fluidPicker);
    }

    @Override
    @Nullable
    public BlockState computeSubstance(int x, int y, int z, double density) {
        boolean lavaLevel = density <= 0.0 && this.fluidPicker.computeFluid(x, y, z).at(y).is(Blocks.LAVA);
        PvAquiferDecision decision = this.decide(x, y, z, density, lavaLevel);
        this.needsFluidTick = decision.needsFluidTick;
        return decision.state;
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return this.needsFluidTick;
    }

    /** What the aquifer places at a block; {@code /pvwg probe} asks this too. */
    public PvAquiferDecision decide(int x, int y, int z, double density, boolean lavaLevel) {
        return PvAquiferRules.decide(x, y, z, density, lavaLevel, this.atPosition);
    }

    /**
     * {@link PvAquiferRules#atPosition} at a block, computed once for the blocks the walls look at; above sea level, the
     * high river's water ({@link #highRiverAt}).
     */
    public PvAquiferDecision atPosition(int x, int y, int z) {
        if (y >= SEA_LEVEL) return this.highRiverAt(x, y, z);
        if (y < MIN_Y) return PvAquiferDecision.AIR;
        int localX = x - this.originX + REACH, localZ = z - this.originZ + REACH;
        if (localX < 0 || localX >= SIDE || localZ < 0 || localZ >= SIDE) {
            return PvAquiferRules.atPosition(x, y, z, this.floodednessField, this.spreadField, this.corridorField, this.dryPathField,
                    this.levelField);
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
        PvAquiferDecision decision = PvAquiferRules.atPosition(x, y, z, this.floodednessField, this.spreadField, this.corridorField,
                this.dryPathField, this.levelField);
        levels[level] = (byte) (decision.ordinal() + 1);
        this.computedPositions++;
        return decision;
    }

    /** F at a block, from lattice inputs; {@code /pvwg probe} shows it next to the exact value. */
    public double floodedness(int x, int y, int z) {
        FloodednessInputs inputs = this.floodednessInputs();
        double entrances = inputs.entrances.at(x, y, z);
        double seaFloodedness = AquiferFormulas.seaFloodedness(y, inputs.depth.at(x, y, z), inputs.continentalness.at(x, y, z),
                entrances, entrances, this.ridge(x, z), inputs.surface.sampleValue(x, y, z));
        double ramen = AquiferFormulas.addsRamen(y, seaFloodedness) ? inputs.ramen.sampleValue(x, y, z) : 0.0;
        return AquiferFormulas.floodedness(y, seaFloodedness, ramen);
    }

    /** S at a block, from the lattice of its inner part. */
    public double spread(int x, int y, int z) {
        return AquiferFormulas.spread(y, this.spreadInputs().basinInner.at(x, y, z));
    }

    /**
     * The flooded corridors at a block of their layers: the final density's noodle inside the corridors' zone, at most
     * 0 where a corridor opens the block; outside the zone no corridor, so the dry noodles there stay dry.
     */
    public double corridor(int x, int y, int z) {
        if (this.corridors == null) {
            this.corridors = this.region(this.spreadConfig.corridors(), BASIN_MIN_Y + 1, CORRIDOR_MAX_Y - 1);
        }
        return this.corridors.at(x, y, z);
    }

    /**
     * The dry noodles at a block of the basin layers: the final density's noodle outside the corridors' zone, at most 0
     * where it opens the block. The basins leave its surroundings dry.
     */
    public double dryPath(int x, int y, int z) {
        if (this.dryPaths == null) {
            this.dryPaths = this.region(this.spreadConfig.dryPaths(), BASIN_MIN_Y + 1, BASIN_MAX_Y - 1);
        }
        return this.dryPaths.at(x, y, z);
    }

    /** The basins' level at a column: the highest y their water, barriers and flooded corridors reach. */
    public double basinLevel(int x, int z) {
        if (this.basinLevel == null) this.basinLevel = new Columns(this.spreadConfig.level());
        return this.basinLevel.at(x, z);
    }

    /**
     * The high river's water at a block, in either layer: at and under the layer's surface, where its valley opens the
     * block (the valley is negative there), water that spills where the ground beside it is open at the surface, the
     * bed's under it; air elsewhere.
     */
    public PvAquiferDecision highRiverAt(int x, int y, int z) {
        if (y >= HIGH_RIVER_MIN_Y && y <= HIGH_RIVER_Y) {
            if (this.valley == null) {
                this.valley = this.region(this.floodednessConfig.highRiver(), HIGH_RIVER_MIN_Y, HIGH_RIVER_Y);
            }
            return riverWater(this.valley.at(x, y, z), y == HIGH_RIVER_Y);
        }
        if (y >= HIGH_RIVER_UPPER_MIN_Y && y <= HIGH_RIVER_UPPER_Y) {
            if (this.upperValley == null) {
                this.upperValley = this.region(this.floodednessConfig.upperHighRiver(), HIGH_RIVER_UPPER_MIN_Y, HIGH_RIVER_UPPER_Y);
            }
            return riverWater(this.upperValley.at(x, y, z), y == HIGH_RIVER_UPPER_Y);
        }
        return PvAquiferDecision.AIR;
    }

    /** A layer's water where its valley opens a block: at its surface, or in its bed. */
    private static PvAquiferDecision riverWater(float valley, boolean surface) {
        if (!(valley < 0.0F)) return PvAquiferDecision.AIR;
        return surface ? PvAquiferDecision.HIGH_RIVER_WATER : PvAquiferDecision.HIGH_RIVER_BED_WATER;
    }

    private double ridge(int x, int z) {
        FloodednessInputs inputs = this.floodednessInputs();
        int localX = x - this.originX + REACH, localZ = z - this.originZ + REACH;
        if (localX < 0 || localX >= SIDE || localZ < 0 || localZ >= SIDE) {
            return inputs.ridge.sampleValue(x, 0, z);
        }
        if (this.ridge == null) {
            this.ridge = new double[SIDE * SIDE];
            Arrays.fill(this.ridge, Double.NaN);
        }
        int index = localX * SIDE + localZ;
        double value = this.ridge[index];
        if (Double.isNaN(value)) {
            value = inputs.ridge.sampleValue(x, 0, z);
            this.ridge[index] = value;
        }
        return value;
    }

    private FloodednessInputs floodednessInputs() {
        if (this.floodedness == null) {
            AquiferFloodedness f = this.floodednessConfig;
            DensitySampler.Bound entrances = this.samplers.get(f.entrances());
            this.floodedness = new FloodednessInputs(
                    this.lattice(this.samplers.get(f.depth()), SEA_BAND_MIN_Y, SEA_LEVEL),
                    this.lattice(this.samplers.get(f.continentalness()), SEA_BAND_MIN_Y, SEA_LEVEL),
                    this.lattice(entrances, SEA_BAND_MIN_Y, SEA_LEVEL),
                    this.samplers.get(f.ridge()), this.samplers.get(f.surface()), this.samplers.get(f.ramen()));
        }
        return this.floodedness;
    }

    private SpreadInputs spreadInputs() {
        if (this.spread == null) {
            Lattice entrances = this.floodednessInputs().entrances;
            DensitySampler.Bound noodle = this.samplers.get(this.spreadConfig.noodle());
            DensitySampler.Bound surface = this.samplers.get(this.spreadConfig.surface());
            this.spread = new SpreadInputs(new Lattice((x, y, z) -> AquiferFormulas.basinInner(y, entrances.exactAt(x, y, z),
                    noodle.sampleValue(x, y, z), surface.sampleValue(x, y, z)), this.originX, this.originZ, BASIN_MIN_Y, BASIN_MAX_Y, CELL_HEIGHT));
        }
        return this.spread;
    }

    private Lattice lattice(DensitySampler.Bound sampler, int minY, int maxY) {
        return new Lattice(Lattice.Source.of(sampler), this.originX, this.originZ, minY, maxY, CELL_HEIGHT);
    }

    private Region region(DensityFunction function, int minY, int maxY) {
        return new Region(this.samplers.get(function), this.regionMinX, minY, this.regionMinZ,
                this.regionMaxX - this.regionMinX + 1, maxY - minY + 1, this.regionMaxZ - this.regionMinZ + 1);
    }

    /**
     * Lattice points sampled so far (depth, continentalness, entrances, then basin inner), for tests and the benchmark;
     * 0 for those not made yet.
     */
    public int[] latticeSamples() {
        FloodednessInputs f = this.floodedness;
        SpreadInputs s = this.spread;
        return new int[]{f == null ? 0 : f.depth.samples(), f == null ? 0 : f.continentalness.samples(), f == null ? 0 : f.entrances.samples(),
                s == null ? 0 : s.basinInner.samples()};
    }

    /** Blocks whose own decision was computed so far (each once), for tests and the benchmark. */
    public int computedPositions() {
        return this.computedPositions;
    }

    /** F's inputs: its smooth ones on lattices, the rest per block or column. */
    private record FloodednessInputs(Lattice depth, Lattice continentalness, Lattice entrances, DensitySampler.Bound ridge,
                                     DensitySampler.Bound surface, DensitySampler.Bound ramen) {
    }

    /** S's inner part on a lattice (from the entrances, the noodle and the surface noise at its points). */
    private record SpreadInputs(Lattice basinInner) {
    }

    /**
     * A function over a box of blocks, sampled as one volume the first time a block in it is asked for (block steps,
     * like the terrain pass's volume, so {@code interpolated} parts give the terrain pass's floats), and per block
     * outside it. The values are copied into a buffer of its own, which the pooled ones can't be kept as.
     */
    /** A function that doesn't depend on y, kept by column for the chunk and {@code REACH} around it. */
    private final class Columns {
        private final DensitySampler.Bound sampler;
        private final double[] values = new double[SIDE * SIDE];

        Columns(DensityFunction function) {
            this.sampler = PvAquifer.this.samplers.get(function);
            Arrays.fill(this.values, Double.NaN);
        }

        double at(int x, int z) {
            int localX = x - PvAquifer.this.originX + REACH, localZ = z - PvAquifer.this.originZ + REACH;
            if (localX < 0 || localX >= SIDE || localZ < 0 || localZ >= SIDE) return this.sampler.sampleValue(x, 0, z);
            int index = localX * SIDE + localZ;
            double value = this.values[index];
            if (Double.isNaN(value)) {
                value = this.sampler.sampleValue(x, 0, z);
                this.values[index] = value;
            }
            return value;
        }
    }

    private static final class Region {
        private final DensitySampler.Bound sampler;
        private final int minX, minY, minZ, sizeX, sizeY, sizeZ;
        @Nullable
        private DensityVolume volume;
        @Nullable
        private DensityBuffer values;

        Region(DensitySampler.Bound sampler, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
            this.sampler = sampler;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }

        float at(int x, int y, int z) {
            int ix = x - this.minX, iy = y - this.minY, iz = z - this.minZ;
            if (ix < 0 || ix >= this.sizeX || iy < 0 || iy >= this.sizeY || iz < 0 || iz >= this.sizeZ) {
                return this.sampler.sampleValue(x, y, z);
            }
            if (this.values == null) {
                DensityVolume volume = new DensityVolume(this.sizeX, this.sizeY, this.sizeZ, this.minX, this.minY, this.minZ, 1, 1, 1);
                DensityBuffer values = DensityBuffer.createUnpooled(volume.size());
                this.sampler.sampleVolume(values, volume);
                this.volume = volume;
                this.values = values;
            }
            return this.values.get(this.volume.indexUnchecked(ix, iy, iz));
        }
    }
}
