package frootloops.versus.mod.environment.worldgen.rocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.core.BlockPos;
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
 * <p>No formation where its middle is under water: rock towers in lakes and seas are another thing.
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

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        long seed = context.random().nextLong();
        RockPlan plan = this.rock.plan(seed);
        int[] anchors = new int[plan.bodies().size()];
        for (int index = 0; index < anchors.length; index++) {
            RockPlan.Body body = plan.bodies().get(index);
            anchors[index] = context.chunkGenerator().getFirstOccupiedHeight(x + body.dx(), z + body.dz(), Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState()) + 1;
        }
        int seaLevel = context.chunkGenerator().getSeaLevel();
        if (anchors[0] < seaLevel - 1) return Optional.empty();
        return Optional.of(new Structure.GenerationStub(new BlockPos(x, anchors[0], z), builder -> {
            for (int index = 0; index < anchors.length; index++) builder.addPiece(new RockFormationPiece(this.rock, seed, index, x, z, anchors[index]));
        }));
    }

    @Override
    public StructureType<?> type() {
        return CustomWorldgen.ROCK_FORMATION;
    }
}
