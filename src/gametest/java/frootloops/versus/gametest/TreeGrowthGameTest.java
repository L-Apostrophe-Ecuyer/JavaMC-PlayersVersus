package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.trees.TreeBlock;
import frootloops.versus.mod.environment.blocks.trees.TreeBlocks;
import frootloops.versus.mod.environment.blocks.trees.TreeRoots;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

/**
 * How trees grow beyond their tree blocks (TreesGameTest): rooted dirt under every trunk, a pale oak's roots going down
 * into the ground and the creaking heart they take down with them, and every tree the game grows, each tree and fallen
 * tree feature of vanilla's and the mod's, made of tree blocks.
 */
public class TreeGrowthGameTest {

    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

    private static Feature feature(ServerLevel level, ResourceKey<Feature> key) {
        return level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(key).value();
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) helper.setBlock(x, y, z, state);
    }

    /** Ground to root in, mid-grove: dirt from y 1 to 8 under grass at y 9. */
    private static void soil(GameTestHelper helper) {
        fill(helper, 16, 1, 16, 32, 8, 32, Blocks.DIRT.defaultBlockState());
        fill(helper, 16, 9, 16, 32, 9, 32, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static BlockState paleOakTree() {
        return CustomBlocks.TREE_BLOCKS.get(Blocks.PALE_OAK_LOG).defaultBlockState();
    }

    /** A grown tree stands on rooted dirt, not dirt. */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void aGrownTreeStandsOnRootedDirt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(feature(level, TreeFeatures.OAK).place(level, level.getChunkSource().getGenerator(), RandomSource.create(42),
                helper.absolutePos(new BlockPos(8, 1, 8))), "the oak didn't grow");
        BlockState under = helper.getBlockState(new BlockPos(8, 0, 8));
        helper.assertTrue(under.is(Blocks.ROOTED_DIRT), "the oak stands on " + under.getBlock().getName().getString() + ", not rooted dirt");
        helper.succeed();
    }

    /** A pale oak's trunk goes on down into the ground: each of its four columns at least ROOT_DEPTH_MIN blocks deep. */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void aPaleOakGrowsRootsDeepIntoTheGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        soil(helper);
        helper.assertTrue(feature(level, TreeFeatures.PALE_OAK).place(level, level.getChunkSource().getGenerator(), RandomSource.create(42),
                helper.absolutePos(new BlockPos(24, 10, 24))), "the pale oak didn't grow");
        List<String> shallow = new ArrayList<>();
        int columns = 0;
        for (BlockPos foot : BlockPos.betweenClosed(20, 10, 20, 28, 10, 28)) {
            BlockState state = helper.getBlockState(foot);
            if (!(state.getBlock() instanceof TreeBlock) || state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) continue;
            columns++;
            int deep = 0;
            while (helper.getBlockState(foot.below(deep + 1)).getBlock() instanceof TreeBlock) deep++;
            if (deep < VersusSettings.Trees.ROOT_DEPTH_MIN) shallow.add(foot.toShortString() + " " + deep);
        }
        helper.assertTrue(columns >= 4, "the pale oak's trunk stands on " + columns + " columns, not four");
        helper.assertTrue(shallow.isEmpty(), "trunk columns rooted less than " + VersusSettings.Trees.ROOT_DEPTH_MIN + " deep: " + shallow);
        helper.succeed();
    }

    /**
     * A creaking heart in a pale oak's trunk goes down into its roots, a tree block back in its place: underground, in a
     * knot of roots that gives it pale oak logs on every side, so it keeps its creaking. Roots show bark against the
     * ground, the cut against the tree.
     */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void aPaleOaksHeartGoesDownIntoItsRoots(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        soil(helper);
        Set<BlockPos> grown = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(24, 10, 24, 25, 15, 25)) {
            helper.setBlock(pos, paleOakTree());
            grown.add(helper.absolutePos(pos));
        }
        helper.setBlock(24, 12, 24, Blocks.CREAKING_HEART.defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                .setValue(CreakingHeartBlock.NATURAL, true));
        float chance = VersusSettings.Trees.HEART_IN_ROOTS;
        VersusSettings.Trees.HEART_IN_ROOTS = 1.0f;
        try {
            TreeRoots.grow(level, RandomSource.create(7), grown);
        } finally {
            VersusSettings.Trees.HEART_IN_ROOTS = chance;
        }
        helper.assertTrue(helper.getBlockState(new BlockPos(24, 12, 24)).getBlock() instanceof TreeBlock, "the heart's old spot in the trunk is "
                + helper.getBlockState(new BlockPos(24, 12, 24)).getBlock().getName().getString());
        List<BlockPos> hearts = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(16, 1, 16, 32, 20, 32)) if (helper.getBlockState(pos).is(Blocks.CREAKING_HEART)) hearts.add(pos.immutable());
        helper.assertTrue(hearts.size() == 1, "there are " + hearts.size() + " creaking hearts, not one");
        BlockPos heart = hearts.getFirst();
        helper.assertTrue(heart.getY() < 10, "the heart stayed above ground, at " + heart.toShortString());
        for (Direction side : Direction.values()) {
            BlockState next = helper.getBlockState(heart.relative(side));
            helper.assertTrue(next.is(BlockTags.PALE_OAK_LOGS), "the buried heart has " + next.getBlock().getName().getString() + " to its " + side.getName());
        }
        BlockState heartState = helper.getBlockState(heart);
        helper.assertTrue(CreakingHeartBlock.hasRequiredLogs(heartState, level, helper.absolutePos(heart)), "the buried heart lacks the logs it needs");
        List<String> wrongFaces = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(16, 1, 16, 32, 9, 32)) {
            BlockState state = helper.getBlockState(pos);
            if (!(state.getBlock() instanceof TreeBlock)) continue;
            for (Direction side : Direction.values()) {
                BlockState next = helper.getBlockState(pos.relative(side));
                boolean cut = next.getBlock() instanceof TreeBlock || next.is(BlockTags.LOGS);
                if (state.getValue(TreeBlock.FACES.get(side)) == cut) wrongFaces.add(pos.toShortString() + " " + side.getName());
            }
        }
        helper.assertTrue(wrongFaces.isEmpty(), "root faces showing the cut against the ground or bark against the tree: " + wrongFaces);
        helper.succeed();
    }

    /**
     * Every tree and fallen tree the game can grow, vanilla's and the mod's, each grown alone in the cleared grove, is
     * made of tree blocks: none of them puts down a vanilla log of a wood that has a tree block.
     */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void everyTreeFeatureGrowsTreeBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Holder.Reference<Feature>> trees = level.registryAccess().lookupOrThrow(Registries.FEATURE).listElements()
                .filter(tree -> tree.value() instanceof TreeFeature || tree.value() instanceof FallenTreeFeature)
                .sorted(Comparator.comparing(tree -> tree.key().identifier().toString())).toList();
        List<String> wrong = new ArrayList<>(), grown = new ArrayList<>(), failed = new ArrayList<>();
        boolean felling = VersusSettings.Trees.DO_FELLING;
        VersusSettings.Trees.DO_FELLING = false;
        try {
            for (Holder.Reference<Feature> tree : trees) {
                String name = tree.key().identifier().toString();
                clear(helper);
                if (!tree.value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), helper.absolutePos(new BlockPos(24, 1, 24)))) {
                    failed.add(name);
                    continue;
                }
                int treeBlocks = 0;
                List<String> logs = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 47, 39, 47)) {
                    BlockState state = helper.getBlockState(pos);
                    if (state.getBlock() instanceof TreeBlock) treeBlocks++;
                    else if (TreeBlocks.forLog(state.getBlock()) != null) logs.add(pos.toShortString() + " " + state.getBlock().getName().getString());
                }
                if (treeBlocks == 0) wrong.add(name + " grew no tree blocks");
                if (!logs.isEmpty()) wrong.add(name + " grew " + logs.size() + " vanilla logs (" + String.join("; ", logs.subList(0, Math.min(3, logs.size()))) + ")");
                grown.add(name + " " + treeBlocks);
            }
            clear(helper);
        } finally {
            VersusSettings.Trees.DO_FELLING = felling;
        }
        helper.assertTrue(wrong.isEmpty(), String.join(", ", wrong));
        helper.assertTrue(grown.size() >= 40, "only " + grown.size() + " of " + trees.size() + " tree features grew; these didn't: " + failed);
        VersusMod.MOD_LOGGER.info("Trees: {} tree features grew tree blocks ({}); these didn't grow here: {}", grown.size(), String.join(", ", grown), failed);
        helper.succeed();
    }

    /** The grove as it was: air over its grass floor, set without updates so nothing reacts. */
    private static void clear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (BlockPos rel : BlockPos.betweenClosed(0, 0, 0, 47, 39, 47)) {
            BlockPos pos = helper.absolutePos(rel);
            BlockState want = rel.getY() == 0 ? grass : air;
            if (!level.getBlockState(pos).equals(want)) level.setBlock(pos, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
