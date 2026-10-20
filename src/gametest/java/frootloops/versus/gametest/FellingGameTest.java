package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.trees.Felling;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How a tree falls, worked out from the blocks (Felling): what a cut leaves with nothing holding it up, which way it
 * goes, which leaves go with it and how much longer the cut takes. Trees built by hand of oak tree blocks on a stone
 * floor, so every number is known; nothing here moves a block.
 */
public class FellingGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static BlockState oakTree() {
        return CustomBlocks.TREE_BLOCKS.get(Blocks.OAK_LOG).defaultBlockState();
    }

    private static BlockState oakLeaf() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) helper.setBlock(x, y, z, state);
    }

    private static void floor(GameTestHelper helper) {
        fill(helper, 0, 0, 0, 13, 0, 7, Blocks.STONE.defaultBlockState());
    }

    /** What would fall were the block at this spot cut, by a cutter standing here (or nobody). */
    private static List<Felling.Fall> cut(GameTestHelper helper, int x, int y, int z, @Nullable Vec3 cutter) {
        return Felling.afterCut(helper.getLevel(), helper.absolutePos(new BlockPos(x, y, z)), cutter == null ? null : helper.absoluteVec(cutter), RandomSource.create(1));
    }

    private static Set<BlockPos> relative(GameTestHelper helper, Set<BlockPos> absolute) {
        return absolute.stream().map(helper::relativePos).collect(Collectors.toSet());
    }

    private static Felling.Fall onlyFall(GameTestHelper helper, List<Felling.Fall> falls) {
        helper.assertTrue(falls.size() == 1, "expected one group to fall, got " + falls.size());
        return falls.getFirst();
    }

    /**
     * Cut at its foot, a trunk fells everything above it: its column with the creaking heart in it, a branch off it, and a
     * branch joined to it by a corner only, as acacias grow them. Cut from the west, it falls east.
     */
    @GameTest(structure = ROOM)
    public void cuttingATrunkFellsEverythingAboveIt(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        helper.setBlock(3, 4, 3, Blocks.CREAKING_HEART.defaultBlockState());
        helper.setBlock(4, 5, 3, oakTree());
        helper.setBlock(5, 5, 3, oakTree());
        helper.setBlock(2, 7, 2, oakTree());
        Felling.Fall fall = onlyFall(helper, cut(helper, 3, 1, 3, new Vec3(0.5, 1.0, 3.5)));
        Set<BlockPos> expected = Set.of(new BlockPos(3, 2, 3), new BlockPos(3, 3, 3), new BlockPos(3, 4, 3), new BlockPos(3, 5, 3),
                new BlockPos(3, 6, 3), new BlockPos(4, 5, 3), new BlockPos(5, 5, 3), new BlockPos(2, 7, 2));
        helper.assertTrue(relative(helper, fall.blocks()).equals(expected), "a trunk cut at its foot fells " + relative(helper, fall.blocks()));
        helper.assertTrue(fall.direction() == Direction.EAST, "cut from the west, it falls " + fall.direction());
        helper.succeed();
    }

    /** A column of vanilla logs never falls: only tree blocks do. */
    @GameTest(structure = ROOM)
    public void vanillaLogsNeverFall(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 5, 3, Blocks.OAK_LOG.defaultBlockState());
        helper.assertTrue(cut(helper, 3, 1, 3, null).isEmpty(), "a column of vanilla logs falls");
        helper.succeed();
    }

    /** A branch cut where it joins its trunk falls alone; the trunk stands on the ground. */
    @GameTest(structure = ROOM)
    public void aBranchCutWhereItJoinsFallsAlone(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        fill(helper, 4, 4, 3, 6, 4, 3, oakTree());
        Felling.Fall fall = onlyFall(helper, cut(helper, 4, 4, 3, null));
        helper.assertTrue(relative(helper, fall.blocks()).equals(Set.of(new BlockPos(5, 4, 3), new BlockPos(6, 4, 3))),
                "a branch cut where it joins fells " + relative(helper, fall.blocks()));
        helper.succeed();
    }

    /** A block put under a branch holds it up when the branch is cut from its trunk. */
    @GameTest(structure = ROOM)
    public void aBranchWithABlockUnderItStays(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        fill(helper, 4, 4, 3, 6, 4, 3, oakTree());
        helper.setBlock(6, 3, 3, Blocks.COBBLESTONE.defaultBlockState());
        helper.assertTrue(cut(helper, 4, 4, 3, null).isEmpty(), "a branch with a block under it falls");
        helper.succeed();
    }

    /**
     * Two oaks seven apart whose crowns meet: the one felled takes only the leaves nearer to it than to the other, and a
     * leaf as near the other stays with it.
     */
    @GameTest(structure = ROOM)
    public void aFelledTreeTakesOnlyTheLeavesNearerToIt(GameTestHelper helper) {
        floor(helper);
        fill(helper, 1, 4, 2, 9, 5, 4, oakLeaf());
        fill(helper, 2, 1, 3, 2, 4, 3, oakTree());
        fill(helper, 8, 1, 3, 8, 4, 3, oakTree());
        Felling.Fall fall = onlyFall(helper, cut(helper, 2, 1, 3, null));
        Set<BlockPos> crown = relative(helper, fall.crown());
        helper.assertTrue(crown.contains(new BlockPos(2, 5, 3)) && crown.contains(new BlockPos(4, 4, 3)), "the felled oak left its own leaves: " + crown);
        helper.assertTrue(crown.stream().allMatch(leaf -> leaf.getX() < 5), "the felled oak took the other's leaves: " + crown);
        helper.succeed();
    }

    /** Cutting a tree block takes 1 + 0.5 sqrt(n) times as long, n what would fall; the top of a trunk, as long as a log. */
    @GameTest(structure = ROOM)
    public void theCutTakesLongerTheMoreThatFalls(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        double base = Felling.cutTimeFactor(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)));
        double top = Felling.cutTimeFactor(helper.getLevel(), helper.absolutePos(new BlockPos(3, 6, 3)));
        helper.assertTrue(Math.abs(base - (1.0 + 0.5 * Math.sqrt(5.0))) < 1.0E-9, "cutting under five tree blocks takes " + base + " times as long");
        helper.assertTrue(top == 1.0, "cutting the top of a trunk takes " + top + " times as long");
        helper.succeed();
    }

    /** A tree that doesn't lean falls straight away from whoever cut it. */
    @GameTest(structure = ROOM)
    public void aBalancedTreeFallsAwayFromItsCutter(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        helper.assertTrue(onlyFall(helper, cut(helper, 3, 1, 3, new Vec3(0.5, 1.0, 3.5))).direction() == Direction.EAST, "cut from the west, it doesn't fall east");
        helper.assertTrue(onlyFall(helper, cut(helper, 3, 1, 3, new Vec3(3.5, 1.0, 0.5))).direction() == Direction.SOUTH, "cut from the north, it doesn't fall south");
        helper.assertTrue(onlyFall(helper, cut(helper, 3, 1, 3, new Vec3(6.5, 1.0, 3.2))).direction() == Direction.WEST, "cut from the east, it doesn't fall west");
        helper.succeed();
    }

    /** A tree whose weight leans more than LEAN off the cut falls that way, whoever cut it. */
    @GameTest(structure = ROOM)
    public void aLeaningTreeFallsToItsHeavySide(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 4, 3, oakTree());
        fill(helper, 4, 4, 3, 8, 4, 3, oakTree());
        Felling.Fall fall = onlyFall(helper, cut(helper, 3, 1, 3, new Vec3(6.5, 1.0, 3.5)));
        helper.assertTrue(fall.direction() == Direction.EAST, "leaning east and cut from the east, it falls " + fall.direction());
        helper.succeed();
    }

    /** A group bigger than MOST_TREE_BLOCKS counts as standing. */
    @GameTest(structure = ROOM)
    public void aTreeTooBigToFollowStands(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        int most = VersusSettings.Trees.MOST_TREE_BLOCKS;
        VersusSettings.Trees.MOST_TREE_BLOCKS = 3;
        try {
            helper.assertTrue(cut(helper, 3, 1, 3, null).isEmpty(), "five tree blocks fell with at most three followed");
        } finally {
            VersusSettings.Trees.MOST_TREE_BLOCKS = most;
        }
        helper.succeed();
    }
}
