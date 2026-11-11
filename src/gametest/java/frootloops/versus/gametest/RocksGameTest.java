package frootloops.versus.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import frootloops.versus.mod.environment.worldgen.rocks.LevelRockCanvas;
import frootloops.versus.mod.environment.worldgen.rocks.RockFeature;
import frootloops.versus.mod.environment.worldgen.rocks.RockFormationPiece;
import frootloops.versus.mod.environment.worldgen.rocks.RockFormationStructure;
import frootloops.versus.mod.environment.worldgen.rocks.RockModel;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan;
import frootloops.versus.mod.environment.worldgen.rocks.RockStamper;
import frootloops.versus.mod.environment.worldgen.rocks.RockTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;

/**
 * Rocks in the world: a rock feature from the data writes its rock within its reach; a formation's body stamped a chunk
 * at a time is the body stamped whole; no grass, plants or vines under water; {@code /pvwg rock} places a rock where it
 * is run.
 */
public class RocksGameTest {

    // 48 x 40 x 48, a grass floor. With sky access: under the barrier ceiling a test area has otherwise, the height map
    // would put the ground on the ceiling
    private static final String GROVE = "players-versus-gametest:leaves_grove";

    private static boolean isRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.TUFF) || state.is(Blocks.ANDESITE) || state.is(Blocks.CLAY);
    }

    /**
     * How many blocks of rock lie in the grove, how far from the middle (24, 24) the furthest one is along x or z, and how
     * many stand on the grove's floor.
     */
    private static int[] rockAround(GameTestHelper helper) {
        int count = 0, far = 0, onFloor = 0;
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 48; z++) {
                for (int y = 1; y < 40; y++) {
                    if (!isRock(helper.getBlockState(new BlockPos(x, y, z)))) continue;
                    count++;
                    if (y == 1) onFloor++;
                    far = Math.max(far, Math.max(Math.abs(x - 24), Math.abs(z - 24)));
                }
            }
        }
        return new int[]{count, far, onFloor};
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void aRockFeaturePlacesItsRockWithinItsReach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.parse("players-versus:rock/boulder"));
        helper.assertTrue(feature instanceof RockFeature, "players-versus:rock/boulder is a rock feature, not " + feature);
        RockFeature boulder = (RockFeature) feature;
        helper.assertTrue(boulder.rock().reach() <= RockFeature.MAX_REACH, "a feature's rock reaches " + boulder.rock().reach());
        boolean placed = boulder.place(level, level.getChunkSource().getGenerator(), RandomSource.create(5), helper.absolutePos(new BlockPos(24, 1, 24)));
        int[] rock = rockAround(helper);
        helper.assertTrue(placed && rock[0] >= 40, "the feature placed " + rock[0] + " blocks of rock");
        helper.assertTrue(rock[2] >= 8, "the rock stands on the floor with " + rock[2] + " blocks");
        helper.assertTrue(rock[1] <= RockFeature.MAX_REACH, "rock " + rock[1] + " blocks from the feature's origin");
        helper.succeed();
    }

    /** The blocks of the box {@code reach} around {@code middle} along x and z, from {@code below} under it to {@code above} over it. */
    private static BlockState[] save(ServerLevel level, BlockPos middle, int reach, int below, int above) {
        int side = 2 * reach + 1;
        BlockState[] states = new BlockState[side * side * (below + above + 1)];
        int index = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -below; dy <= above; dy++) states[index++] = level.getBlockState(middle.offset(dx, dy, dz));
            }
        }
        return states;
    }

    private static void restore(ServerLevel level, BlockPos middle, int reach, int below, int above, BlockState[] states) {
        int index = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -below; dy <= above; dy++) level.setBlock(middle.offset(dx, dy, dz), states[index++], Block.UPDATE_CLIENTS);
            }
        }
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void aBodyStampedAChunkAtATimeIsTheBodyStampedWhole(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RockPlan plan = RockTemplate.fromJson(JsonParser.parseString("{\"bodies\": [{\"shape\": \"boulder\", \"height\": 6, \"radius\": 4}]}").getAsJsonObject()).plan(9);
        RockModel model = RockModel.build(plan.bodies().getFirst());
        int reach = model.reach(), below = 4, above = 16;
        helper.assertTrue(reach <= 23, "the boulder fits in the grove: it reaches " + reach);
        // Both on the same ground: stamped whole, then the ground put back and stamped in four boxes, split across its middle, the last first
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        BlockState[] ground = save(level, middle, reach, below, above);
        RockStamper.stamp(model, middle.getX(), middle.getY(), middle.getZ(), new LevelRockCanvas(level, plan.palette()),
                middle.getX() - reach, middle.getZ() - reach, middle.getX() + reach, middle.getZ() + reach);
        BlockState[] whole = save(level, middle, reach, below, above);
        restore(level, middle, reach, below, above, ground);
        int[][] boxes = {{1, reach, 1, reach}, {-reach, 0, 1, reach}, {1, reach, -reach, 0}, {-reach, 0, -reach, 0}};
        for (int[] box : boxes) {
            RockStamper.stamp(model, middle.getX(), middle.getY(), middle.getZ(), new LevelRockCanvas(level, plan.palette()),
                    middle.getX() + box[0], middle.getZ() + box[2], middle.getX() + box[1], middle.getZ() + box[3]);
        }
        BlockState[] pieces = save(level, middle, reach, below, above);
        int rock = 0, side = 2 * reach + 1, height = below + above + 1;
        for (int index = 0; index < whole.length; index++) {
            int dx = index / (side * height) - reach, dz = index / height % side - reach, dy = index % height - below;
            helper.assertTrue(whole[index] == pieces[index], "at " + dx + " " + dy + " " + dz + " from the middle: " + whole[index] + " whole, " + pieces[index] + " in pieces");
            if (isRock(whole[index])) rock++;
        }
        helper.assertTrue(rock >= 40, "the boulder is " + rock + " blocks of rock");
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void aSavedFormationPieceDrawsTheSameRockWithinItsBox(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The second body of a formation stands away from its middle
        RockTemplate template = RockTemplate.fromJson(JsonParser.parseString("{\"bodies\": [{\"shape\": \"boulder\", \"height\": 5, \"radius\": 3.5},"
                + " {\"shape\": \"boulder\", \"height\": 6, \"radius\": 4, \"distance\": 7}]}").getAsJsonObject());
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        RockFormationPiece piece = new RockFormationPiece(template, 11, 1, middle.getX(), middle.getZ(), middle.getY());
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level);
        StructurePiece loaded = CustomWorldgen.ROCK_FORMATION_PIECE.load(context, piece.createTag(context));
        BoundingBox box = piece.getBoundingBox();
        helper.assertTrue(loaded instanceof RockFormationPiece && loaded.getBoundingBox().equals(box), "the piece loaded is " + loaded + " in " + loaded.getBoundingBox() + ", not in " + box);
        int reach = 22, below = 4, above = 20;
        BoundingBox everywhere = new BoundingBox(middle.getX() - reach, level.getMinY(), middle.getZ() - reach, middle.getX() + reach, level.getMaxY(), middle.getZ() + reach);
        ChunkPos chunk = ChunkPos.containing(middle);
        BlockState[] before = save(level, middle, reach, below, above);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1), everywhere, chunk, middle);
        BlockState[] drawn = save(level, middle, reach, below, above);
        restore(level, middle, reach, below, above, before);
        loaded.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(2), everywhere, chunk, middle);
        BlockState[] again = save(level, middle, reach, below, above);
        int rock = 0, outside = 0, side = 2 * reach + 1, height = below + above + 1;
        for (int index = 0; index < drawn.length; index++) {
            int dx = index / (side * height) - reach, dz = index / height % side - reach, dy = index % height - below;
            helper.assertTrue(drawn[index] == again[index], "at " + dx + " " + dy + " " + dz + " from the middle: " + drawn[index] + " drawn, " + again[index] + " drawn again");
            if (drawn[index] == before[index]) continue;
            if (isRock(drawn[index])) rock++;
            if (!box.isInside(middle.offset(dx, dy, dz))) outside++;
        }
        helper.assertTrue(rock >= 30, "the piece drew " + rock + " blocks of rock");
        helper.assertTrue(outside == 0, outside + " blocks drawn outside the piece's box " + box);
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void badRockDataFailsToLoadSayingWhatIsWrong(GameTestHelper helper) {
        String[][] cases = {
                {"{\"bodies\": [{\"shape\": \"boulder\"}], \"palette\": {\"base\": \"minecraft:no_such_stone\"}}", "minecraft:no_such_stone"},
                {"{\"bodies\": [{\"shape\": \"boulder\", \"height\": -3}]}", "bodies[0].height"},
                {"{\"bodies\": [{\"shape\": \"massif\"}]}", "reach"},
        };
        for (String[] bad : cases) {
            var result = RockFeature.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"rock\": " + bad[0] + "}"));
            String message = result.error().map(error -> error.message()).orElse("");
            helper.assertTrue(result.isError() && message.contains(bad[1]), bad[0] + " loaded as " + result + ": the error doesn't name " + bad[1]);
        }
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void nothingGrowsOnARockUnderWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // A pond three deep around the middle, its floor the grove's
        for (int x = 8; x < 40; x++) {
            for (int z = 8; z < 40; z++) {
                for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
            }
        }
        RockPlan plan = RockTemplate.fromJson(JsonParser.parseString("{\"bodies\": [{\"shape\": \"dome\", \"height\": 9, \"radius\": 7, \"cover\": 1}]}").getAsJsonObject()).plan(3);
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, middle.getX(), middle.getZ()) - middle.getY() + 1;
        RockStamper.stamp(plan, middle.getX(), middle.getZ(), new LevelRockCanvas(level, plan.palette()), middle.getX() - 16, middle.getZ() - 16, middle.getX() + 16, middle.getZ() + 16);
        int underWater = 0, above = 0;
        StringBuilder levels = new StringBuilder();
        for (int y = 1; y < 30; y++) {
            int rock = 0, green = 0;
            for (int x = 8; x < 40; x++) {
                for (int z = 8; z < 40; z++) {
                    BlockState state = helper.getBlockState(new BlockPos(x, y, z));
                    if (isRock(state)) rock++;
                    boolean cover = state.is(BlockTags.GRASS_BLOCKS) || state.is(BlockTags.MOSS_BLOCKS);
                    boolean plant = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.BUSH) || state.is(Blocks.VINE);
                    if (!cover && !plant) continue;
                    green++;
                    // Grass or moss under water has water over it (at the water's face it is a shore); a plant grows in the pond's water
                    boolean wet = cover ? helper.getBlockState(new BlockPos(x, y + 1, z)).is(Blocks.WATER) : y <= 3;
                    if (wet) underWater++;
                    else if (y > 3) above++;
                }
            }
            if (rock + green > 0) levels.append(" y").append(y).append(": ").append(rock).append(" rock, ").append(green).append(" green;");
        }
        helper.assertTrue(underWater == 0, underWater + " blocks of grass, moss, plants or vines under the water:" + levels);
        helper.assertTrue(above > 0, "the dome has plants above the water; its ground at " + ground + " in the grove:" + levels);
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void theCommandPlacesARockWhereItIsRun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level).withPosition(Vec3.atBottomCenterOf(middle)).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source, "pvwg rock players-versus:rock/boulder 7");
        int[] rock = rockAround(helper);
        helper.assertTrue(rock[0] >= 40, "the command placed " + rock[0] + " blocks of rock");
        helper.assertTrue(rock[2] >= 8, "the rock stands on the floor with " + rock[2] + " blocks");
        helper.assertTrue(rock[1] <= RockFeature.MAX_REACH, "rock " + rock[1] + " blocks from where it was run");
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void theFormationsAreRockFormations(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String id : new String[]{"players-versus:rock/monolith", "players-versus:rock/spires", "players-versus:rock/great_boulders"}) {
            Structure structure = structures.getValue(Identifier.parse(id));
            helper.assertTrue(structure instanceof RockFormationStructure, id + " is a rock formation, not " + structure);
        }
        helper.succeed();
    }
}
