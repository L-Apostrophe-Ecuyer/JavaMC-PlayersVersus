package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.debug.WorldgenCensus;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Structure type {@code players-versus:rock_formation}: a large rock formation (spires, a monolith, great boulders) on
 * the world's grid, its structure set's {@code random_spread} placing one in some cells of it. The formation is drawn
 * from its template and the structure's random for the chunk; each body is a piece ({@link RockFormationPiece}) that
 * stands on the terrain's height at its own middle and draws, chunk by chunk, only the columns of the chunk being made.
 * In the data, a structure's usual settings and {@code "rock": {...}}, as a feature's.
 *
 * <p>No formation where its middle is under water (rock towers in lakes and seas are another thing), nor where rock may
 * not stand there ({@link RockGround}: above the mountainsides, out of the 3D-noise terrain, on level ground); its biomes
 * decide the rest, cold ones too.
 */
public class RockFormationStructure extends Structure {

    public static final MapCodec<RockFormationStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            RockFeature.TEMPLATE_CODEC.fieldOf("rock").forGetter(RockFormationStructure::rock)
    ).apply(instance, RockFormationStructure::new));

    private final RockTemplate rock;

    public RockFormationStructure(Structure.StructureSettings settings, RockTemplate rock) {
        super(settings);
        this.rock = rock;
    }

    public RockTemplate rock() {
        return this.rock;
    }

    /**
     * The main body's ground first, and the biome there (the structure's biomes, as the game checks them once a structure has
     * its place): the structure sets try their formations one after another in every cell of the world's grid, and a formation
     * out of its biomes stops after one column of terrain read, not the twenty of its ground and its bodies' places.
     */
    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        long seed = context.random().nextLong();
        RockPlan plan = this.rock.plan(seed);
        int[] anchors = new int[plan.bodies().size()];
        anchors[0] = anchor(context, x, z, plan.bodies().get(0));
        if (anchors[0] < context.chunkGenerator().getSeaLevel() - 1) return Optional.empty();
        if (!context.validBiome().test(context.noiseBiomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(anchors[0]), QuartPos.fromBlock(z)))) {
            return Optional.empty();
        }
        for (int index = 1; index < anchors.length; index++) anchors[index] = anchor(context, x, z, plan.bodies().get(index));
        boolean ground = this.onRockGround(context, x, anchors[0], z);
        WorldgenCensus.rockGround(ground ? "a formation kept" : "a formation refused");
        if (!ground) return Optional.empty();
        return Optional.of(new Structure.GenerationStub(new BlockPos(x, anchors[0], z), builder -> {
            for (int index = 0; index < anchors.length; index++) builder.addPiece(new RockFormationPiece(this.rock, seed, index, x, z, anchors[index]));
        }));
    }

    /** Where a body stands: on the ground at its middle. */
    private static int anchor(Structure.GenerationContext context, int x, int z, RockPlan.Body body) {
        return context.chunkGenerator().getFirstOccupiedHeight(x + body.dx(), z + body.dz(), Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
                context.randomState()) + 1;
    }

    /**
     * Whether a formation may stand in this climate: its ground's ({@link RockGround#terrain}), whatever the temperature, as the
     * structure's biomes (its tag) decide where it goes, cold places too.
     */
    static boolean suits(Climate.TargetPoint climate) {
        return RockGround.terrain(Climate.unquantizeCoord(climate.humidity()), Climate.unquantizeCoord(climate.erosion()));
    }

    /** Whether rock may stand at the main body's middle: its climate's ground, and level ground around it ({@link RockGround}). */
    private boolean onRockGround(Structure.GenerationContext context, int x, int y, int z) {
        Climate.TargetPoint climate = context.climateSampler().sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
        if (!suits(climate)) return false;
        int[] heights = new int[RockGround.AROUND.length];
        for (int index = 0; index < heights.length; index++) {
            heights[index] = context.chunkGenerator().getFirstOccupiedHeight(x + RockGround.AROUND[index][0], z + RockGround.AROUND[index][1], Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
        }
        return RockGround.level(heights);
    }

    @Override
    public StructureType<?> type() {
        return CustomWorldgen.ROCK_FORMATION;
    }
}
