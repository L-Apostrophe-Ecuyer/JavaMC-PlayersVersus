package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.trees.Felling;
import frootloops.versus.mod.environment.blocks.trees.TreeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How a tree falls: first as worked out from the blocks (Felling), what a cut leaves with nothing holding it up, which
 * way it goes, which leaves go with it and how much longer the cut takes, on trees built by hand of oak tree blocks so
 * every number is known; then as it happens (TreeFelling), a player's cut bringing a tree down as falling blocks that
 * land as a fallen log, hurt what they land on and spare pets.
 */
public class FellingGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8
    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

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

    /** These spots in the test's structure, where they are in the world: the falls are told in the world's. */
    private static Set<BlockPos> absolute(GameTestHelper helper, BlockPos... relative) {
        return Arrays.stream(relative).map(helper::absolutePos).collect(Collectors.toSet());
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
        Set<BlockPos> expected = absolute(helper, new BlockPos(3, 2, 3), new BlockPos(3, 3, 3), new BlockPos(3, 4, 3), new BlockPos(3, 5, 3),
                new BlockPos(3, 6, 3), new BlockPos(4, 5, 3), new BlockPos(5, 5, 3), new BlockPos(2, 7, 2));
        helper.assertTrue(fall.blocks().equals(expected), "a trunk cut at its foot fells " + fall.blocks() + ", not " + expected);
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
        Set<BlockPos> branch = absolute(helper, new BlockPos(5, 4, 3), new BlockPos(6, 4, 3));
        helper.assertTrue(fall.blocks().equals(branch), "a branch cut where it joins fells " + fall.blocks() + ", not " + branch);
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
        Set<BlockPos> crown = fall.crown();
        helper.assertTrue(crown.containsAll(absolute(helper, new BlockPos(2, 5, 3), new BlockPos(4, 4, 3))), "the felled oak left its own leaves: " + crown);
        Set<BlockPos> others = new HashSet<>();
        for (BlockPos leaf : BlockPos.betweenClosed(5, 4, 2, 9, 5, 4)) others.add(helper.absolutePos(leaf));
        helper.assertTrue(crown.stream().noneMatch(others::contains), "the felled oak took leaves as near the other or nearer: " + crown);
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

    private static int count(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Predicate<BlockState> what) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) if (what.test(helper.getBlockState(pos))) count++;
        return count;
    }

    /** A player standing here breaks the block at this spot, as they would in survival. */
    private static void cutAsAPlayer(GameTestHelper helper, int x, int y, int z, Vec3 standing) {
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        Vec3 at = helper.absoluteVec(standing);
        player.setPos(at.x, at.y, at.z);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(x, y, z)));
    }

    /** A player cutting the foot of a trunk breaks it 1 + 0.5 sqrt(n) times slower than its top, n what would fall. */
    @GameTest(structure = ROOM)
    public void cuttingUnderATreeIsSlowerThanAtItsTop(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        BlockPos foot = new BlockPos(3, 1, 3), top = new BlockPos(3, 6, 3);
        float atFoot = helper.getBlockState(foot).getDestroyProgress(player, helper.getLevel(), helper.absolutePos(foot));
        float atTop = helper.getBlockState(top).getDestroyProgress(player, helper.getLevel(), helper.absolutePos(top));
        double slower = atTop / atFoot;
        helper.assertTrue(Math.abs(slower - (1.0 + 0.5 * Math.sqrt(5.0))) < 1.0E-3, "the foot of a six-block trunk breaks " + slower + " times slower than its top");
        helper.succeed();
    }

    /** Cut at its foot by a player, a trunk creaks and stands a moment, then comes down away from them as a fallen log. */
    @GameTest(structure = ROOM, maxTicks = 120)
    public void aCutTreeCreaksThenFallsAwayFromItsCutter(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        cutAsAPlayer(helper, 3, 1, 3, new Vec3(0.5, 1.0, 3.5));
        helper.runAfterDelay(5, () -> helper.assertTrue(count(helper, 3, 2, 3, 3, 6, 3, state -> state.getBlock() instanceof TreeBlock) == 5,
                "the tree came down before it had creaked its warning"));
        helper.runAfterDelay(VersusSettings.Trees.FELLING_DELAY + 60, () -> {
            helper.assertTrue(count(helper, 0, 1, 0, 13, 7, 7, state -> state.getBlock() instanceof TreeBlock) == 0, "the cut tree is still standing");
            List<String> wrong = new ArrayList<>();
            int logs = 0;
            for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 13, 7, 7)) {
                BlockState state = helper.getBlockState(pos);
                if (!state.is(Blocks.OAK_LOG)) continue;
                logs++;
                if (state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.X || pos.getX() <= 3 || pos.getY() != 1) wrong.add(pos.toShortString());
            }
            helper.assertTrue(logs >= 3, "the felled trunk left " + logs + " logs on the ground");
            helper.assertTrue(wrong.isEmpty(), "logs that don't lie east of the stump along x, on the ground: " + wrong);
            helper.succeed();
        });
    }

    /** Two grown oaks: cut from the west, one falls east; cut from the north, the other falls south, crowns and all. */
    @GameTest(structure = GROVE, maxTicks = 200)
    public void aGrownOakFallsAwayFromWhoeverCutIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature oak = level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(TreeFeatures.OAK).value();
        helper.assertTrue(oak.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), helper.absolutePos(new BlockPos(8, 1, 8))), "the first oak didn't grow");
        helper.assertTrue(oak.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), helper.absolutePos(new BlockPos(32, 1, 32))), "the second oak didn't grow");
        cutAsAPlayer(helper, 8, 1, 8, new Vec3(5.5, 1.0, 8.5));
        cutAsAPlayer(helper, 32, 1, 32, new Vec3(32.5, 1.0, 29.5));
        helper.runAfterDelay(VersusSettings.Trees.FELLING_DELAY + 100, () -> {
            assertFelled(helper, 0, 0, 23, 23, 8, 8, Direction.EAST);
            assertFelled(helper, 24, 24, 47, 47, 32, 32, Direction.SOUTH);
            helper.succeed();
        });
    }

    /**
     * In this square of the grove the oak cut at (x, 1, z) is down: no tree block or oak leaf left, and at least three
     * logs on the ground, every one lying that way of its stump and along that way.
     */
    private static void assertFelled(GameTestHelper helper, int x0, int z0, int x1, int z1, int x, int z, Direction way) {
        int treeBlocks = 0, leaves = 0, logs = 0;
        List<String> wrong = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(x0, 1, z0, x1, 30, z1)) {
            BlockState state = helper.getBlockState(pos);
            if (state.getBlock() instanceof TreeBlock) treeBlocks++;
            else if (state.is(Blocks.OAK_LEAVES)) leaves++;
            else if (state.is(Blocks.OAK_LOG)) {
                logs++;
                boolean beyond = (pos.getX() - x) * way.getStepX() + (pos.getZ() - z) * way.getStepZ() > 0;
                if (state.getValue(RotatedPillarBlock.AXIS) != way.getAxis() || !beyond) wrong.add(pos.toShortString());
            }
        }
        helper.assertTrue(treeBlocks == 0, "cut to fall " + way + ", " + treeBlocks + " tree blocks still stand");
        helper.assertTrue(leaves == 0, "cut to fall " + way + ", " + leaves + " oak leaves are left");
        helper.assertTrue(logs >= 3, "cut to fall " + way + ", the oak left " + logs + " logs on the ground");
        helper.assertTrue(wrong.isEmpty(), "cut to fall " + way + ", logs that don't lie that way along it: " + wrong);
    }

    /** A falling log hurts what it lands on, as an anvil does, but never kills a tamed wolf, nor a player. */
    @GameTest(structure = GROVE, maxTicks = 200)
    public void aFallingTreeHurtsWhatItLandsOnButSparesPets(GameTestHelper helper) {
        fill(helper, 8, 1, 8, 8, 9, 8, oakTree());
        ServerPlayer owner = FakePlayer.get(helper.getLevel());
        List<Mob> husks = new ArrayList<>();
        List<TamableAnimal> wolves = new ArrayList<>();
        for (int x = 10; x <= 16; x++) {
            husks.add(helper.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(x + 0.5, 1.0, 8.5)));
            TamableAnimal wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new Vec3(x + 0.5, 1.0, 8.5));
            wolf.tame(owner);
            wolf.setHealth(1.0f);
            wolves.add(wolf);
        }
        cutAsAPlayer(helper, 8, 1, 8, new Vec3(5.5, 1.0, 8.5));
        helper.runAfterDelay(VersusSettings.Trees.FELLING_DELAY + 100, () -> {
            double hurt = husks.stream().mapToDouble(husk -> husk.getMaxHealth() - husk.getHealth()).sum();
            helper.assertTrue(hurt > 0.0, "the falling tree hurt none of the husks it landed on");
            helper.assertTrue(wolves.stream().allMatch(LivingEntity::isAlive), "the falling tree killed a tamed wolf");
            helper.succeed();
        });
    }

    /** What a felled tree carries breaks as if mined: the bee nest on it and the creaking heart in its trunk. */
    @GameTest(structure = ROOM, maxTicks = 120)
    public void aFelledTreesBeeNestAndHeartBreak(GameTestHelper helper) {
        floor(helper);
        fill(helper, 3, 1, 3, 3, 6, 3, oakTree());
        helper.setBlock(3, 4, 3, Blocks.CREAKING_HEART.defaultBlockState());
        helper.setBlock(3, 5, 4, Blocks.BEE_NEST.defaultBlockState());
        cutAsAPlayer(helper, 3, 1, 3, new Vec3(0.5, 1.0, 3.5));
        helper.runAfterDelay(VersusSettings.Trees.FELLING_DELAY + 60, () -> {
            helper.assertTrue(count(helper, 0, 1, 0, 13, 7, 7, state -> state.is(Blocks.BEE_NEST)) == 0, "the felled tree's bee nest is still there");
            helper.assertTrue(count(helper, 0, 1, 0, 13, 7, 7, state -> state.is(Blocks.CREAKING_HEART)) == 0, "the felled tree's creaking heart is still there");
            helper.succeed();
        });
    }
}
