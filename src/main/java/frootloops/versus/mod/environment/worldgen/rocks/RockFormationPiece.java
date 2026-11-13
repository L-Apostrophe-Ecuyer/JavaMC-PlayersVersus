package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One body of a rock formation ({@link RockFormationStructure}): its template, the formation's seed and its own place
 * in it, which is all it saves; its model is built again from those when it is first needed, and kept while the piece
 * is, as each chunk it reaches asks it for that chunk's columns (from the generator's threads at once, so it is built
 * under a lock).
 */
public class RockFormationPiece extends StructurePiece {

    private final RockTemplate rock;
    private final long seed;
    private final int body;
    private final int originX, originZ, anchorY;
    private RockModel model;

    public RockFormationPiece(RockTemplate rock, long seed, int body, int originX, int originZ, int anchorY) {
        super(CustomWorldgen.ROCK_FORMATION_PIECE, 0, box(rock.plan(seed).bodies().get(body), originX, originZ, anchorY));
        this.rock = rock;
        this.seed = seed;
        this.body = body;
        this.originX = originX;
        this.originZ = originZ;
        this.anchorY = anchorY;
    }

    public RockFormationPiece(CompoundTag tag) {
        super(CustomWorldgen.ROCK_FORMATION_PIECE, tag);
        this.rock = RockTemplate.fromJson(JsonParser.parseString(tag.getStringOr("Rock", "{}")).getAsJsonObject());
        this.seed = tag.getLongOr("Seed", 0);
        this.body = tag.getIntOr("Body", 0);
        this.originX = tag.getIntOr("X", 0);
        this.originZ = tag.getIntOr("Z", 0);
        this.anchorY = tag.getIntOr("Y", 0);
    }

    /** Everything the body can write: its rock and its plants, its foot filled down to the ground, the ground risen around it. */
    private static BoundingBox box(RockPlan.Body body, int originX, int originZ, int anchorY) {
        int[] bounds = RockModel.bounds(body);
        int x = originX + body.dx(), z = originZ + body.dz();
        return new BoundingBox(x + bounds[0], anchorY + bounds[1] - RockStamper.MAX_FILL, z + bounds[2], x + bounds[3], anchorY + bounds[4], z + bounds[5]);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Rock", this.rock.toJson().toString());
        tag.putLong("Seed", this.seed);
        tag.putInt("Body", this.body);
        tag.putInt("X", this.originX);
        tag.putInt("Z", this.originZ);
        tag.putInt("Y", this.anchorY);
    }

    private synchronized RockModel model() {
        if (this.model == null) this.model = RockModel.build(this.rock.plan(this.seed).bodies().get(this.body));
        return this.model;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
                            ChunkPos chunkPos, BlockPos referencePos) {
        RockModel model = this.model();
        RockPlan.Body body = model.body();
        LevelRockCanvas canvas = new LevelRockCanvas(level, this.rock.palette());
        RockStamper.stamp(model, this.originX + body.dx(), this.anchorY, this.originZ + body.dz(), canvas, chunkBox.minX(), chunkBox.minZ(),
                chunkBox.maxX(), chunkBox.maxZ());
    }
}
