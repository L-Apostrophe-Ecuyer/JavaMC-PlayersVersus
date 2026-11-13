package frootloops.versus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;

/**
 * The wetlands: the clumps of waterlogged leaves that float at the water line of the swamps' pools.
 */
public class WetlandsGameTest {

    // 48 x 40 x 48, a grass floor, with sky access so that the height map finds the water and not the barrier ceiling
    private static final String GROVE = "players-versus-gametest:leaves_grove";
    /**
     * The pond: 12 blocks across, ringed by two blocks of grass as high as its water, in the chunk around the grove's middle;
     * three deep, but for its west part ({@link #SHALLOW} blocks from the chunk's west edge on), one deep over mud.
     */
    private static final int RING = 2, DEPTH = 3, SHALLOW = 8, RUNS = 40;

    /**
     * Digs the pond into the chunk holding the grove's middle (the whole chunk lies in the grove): mud for its floor (the
     * grove's floor), water {@link #DEPTH} deep (one deep over mud in its shallow part), a ring of grass level with the water.
     * Returns the chunk's corner x, z and the floor's y.
     */
    private static int[] pond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        int x0 = middle.getX() & ~15, z0 = middle.getZ() & ~15, floor = middle.getY() - 1;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                for (int y = floor; y < floor + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), pondBlock(x - x0, y - floor, z - z0), Block.UPDATE_CLIENTS);
                }
            }
        }
        return new int[]{x0, z0, floor};
    }

    /** What the pond has at (x, y, z) of its chunk, y over its floor (0, mud or the ring's grass). */
    private static BlockState pondBlock(int x, int y, int z) {
        boolean ring = x < RING || x > 15 - RING || z < RING || z > 15 - RING;
        if (y == 0) return (ring ? Blocks.GRASS_BLOCK : Blocks.MUD).defaultBlockState();
        if (y > DEPTH) return Blocks.AIR.defaultBlockState();
        if (ring) return Blocks.GRASS_BLOCK.defaultBlockState();
        return (x < SHALLOW && y < DEPTH ? Blocks.MUD : Blocks.WATER).defaultBlockState();
    }

    /**
     * Runs the placed feature {@link #RUNS} times over the pond and returns the leaves it made, checking each: waterlogged and
     * persistent, in the water's top layer, with air above and water below, inside the ring; the ring and the floor are as they
     * were and nothing grew in the air above.
     */
    private static List<BlockState> leavesOver(GameTestHelper helper, String name) {
        ServerLevel level = helper.getLevel();
        PlacedFeature feature = PlacedFeatures.withoutBiomeCheck(helper, name, 4);
        int[] pond = pond(helper);
        int x0 = pond[0], z0 = pond[1], floor = pond[2], surface = floor + DEPTH;
        for (int run = 0; run < RUNS; run++) {
            // From the chunk's corner: the placement spreads the leaves over the chunk from there
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(run), new BlockPos(x0, surface + 1, z0));
        }
        List<BlockState> leaves = new ArrayList<>();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                boolean ring = x - x0 < RING || x - x0 > 15 - RING || z - z0 < RING || z - z0 > 15 - RING;
                for (int y = floor; y < floor + 12; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(at);
                    String where = name + " at " + (x - x0) + ", " + (y - floor) + ", " + (z - z0) + ": ";
                    if (!state.is(BlockTags.LEAVES)) {
                        // Nothing else was changed
                        BlockState expected = pondBlock(x - x0, y - floor, z - z0);
                        helper.assertTrue(state == expected, where + state + " where " + expected + " was");
                        continue;
                    }
                    helper.assertTrue(!ring, where + "leaves on the ring of grass");
                    helper.assertTrue(y == surface, where + "leaves off the water line (at " + (surface - floor) + ")");
                    helper.assertTrue(state.getValue(LeavesBlock.WATERLOGGED), where + "leaves that aren't waterlogged");
                    helper.assertTrue(state.getValue(LeavesBlock.PERSISTENT), where + "leaves that aren't persistent");
                    helper.assertTrue(level.getBlockState(at.above()).isAir(), where + "leaves under something");
                    helper.assertTrue(level.getBlockState(at.below()).is(Blocks.WATER), where + "leaves over no water (one deep, or none)");
                    leaves.add(state);
                }
            }
        }
        return leaves;
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void waterloggedLeavesFloatAtTheWaterLine(GameTestHelper helper) {
        // The swamp's and the grassy wetlands' (each placed by its own trees' noise), oak both
        for (String name : List.of("waterlogged_leaves", "waterlogged_leaves_wetlands")) {
            List<BlockState> leaves = leavesOver(helper, name);
            helper.assertTrue(leaves.size() >= 3, "only " + leaves.size() + " leaves floated on the pond: " + name);
            helper.assertTrue(leaves.stream().allMatch(state -> state.is(Blocks.OAK_LEAVES)), name + "'s leaves are oak: " + leaves);
        }
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 20, skyAccess = true)
    public void theColdSwampsLeavesAreSpruceAndBirch(GameTestHelper helper) {
        List<BlockState> leaves = leavesOver(helper, "waterlogged_leaves_cold");
        helper.assertTrue(leaves.size() >= 3, "only " + leaves.size() + " leaves floated on the pond");
        long spruce = leaves.stream().filter(state -> state.is(Blocks.SPRUCE_LEAVES)).count(), birch = leaves.stream().filter(state -> state.is(Blocks.BIRCH_LEAVES)).count();
        helper.assertTrue(spruce + birch == leaves.size(), "the cold swamp's leaves are spruce and birch: " + leaves);
        helper.assertTrue(spruce > birch && birch > 0, spruce + " spruce and " + birch + " birch: mostly spruce, some birch");
        helper.succeed();
    }
}
