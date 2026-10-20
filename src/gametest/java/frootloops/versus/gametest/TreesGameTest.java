package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The tree block: what every growing tree is made of, one per wood, with a face for each side that shows bark or the
 * cut wood under it, set once as the tree grows (or as one is placed in creative) and never after. Breaking one gives
 * its wood's vanilla log, and to the game it is a log of its wood. Looked up by id, so these build without it.
 */
public class TreesGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room";   // 14 x 8 x 8
    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

    /** Every wood with a tree block, and its vanilla log. */
    private record Wood(String name, Block log) {}

    private static final List<Wood> WOODS = List.of(new Wood("oak", Blocks.OAK_LOG), new Wood("spruce", Blocks.SPRUCE_LOG),
            new Wood("birch", Blocks.BIRCH_LOG), new Wood("jungle", Blocks.JUNGLE_LOG), new Wood("acacia", Blocks.ACACIA_LOG),
            new Wood("cherry", Blocks.CHERRY_LOG), new Wood("dark_oak", Blocks.DARK_OAK_LOG), new Wood("pale_oak", Blocks.PALE_OAK_LOG),
            new Wood("mangrove", Blocks.MANGROVE_LOG), new Wood("poplar", Blocks.POPLAR_LOG));

    /** This wood's tree block, failing the test when there is none. */
    private static Block treeBlock(GameTestHelper helper, String wood) {
        Identifier id = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, wood + "_tree");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), id + " isn't a block");
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    private static boolean isTreeBlock(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(VersusMod.MOD_ID) && id.getPath().endsWith("_tree");
    }

    /** Whether this side of a tree block shows bark (true) or the cut (false). */
    private static boolean bark(BlockState state, Direction side) {
        return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
    }

    /** The rule a face is set by: cut against a tree block, a log or a sturdy face, bark against anything else. */
    private static boolean shouldBeBark(ServerLevel level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        BlockState neighbour = level.getBlockState(next);
        return !(isTreeBlock(neighbour) || neighbour.is(BlockTags.LOGS) || neighbour.isFaceSturdy(level, next, side.getOpposite()));
    }

    private static String faces(BlockState state) {
        StringBuilder faces = new StringBuilder();
        for (Direction side : Direction.values()) faces.append(side.getName().charAt(0)).append(bark(state, side) ? "" : "-cut").append(' ');
        return faces.toString().trim();
    }

    /** Every tree grows its wood's tree blocks, not vanilla logs, each face set as the rule says. */
    @GameTest(structure = GROVE, maxTicks = 40)
    public void everyTreeGrowsTreeBlocks(GameTestHelper helper) {
        treesGrowTreeBlocks(helper, List.of(TreeFeatures.OAK, TreeFeatures.FANCY_OAK, TreeFeatures.BIRCH, TreeFeatures.SPRUCE, TreeFeatures.PINE,
                TreeFeatures.ACACIA, TreeFeatures.DARK_OAK, TreeFeatures.SWAMP_OAK, TreeFeatures.JUNGLE_BUSH));
    }

    /** The rest of the trees, as everyTreeGrowsTreeBlocks. */
    @GameTest(structure = GROVE, maxTicks = 40)
    public void everyOtherTreeGrowsTreeBlocks(GameTestHelper helper) {
        treesGrowTreeBlocks(helper, List.of(TreeFeatures.JUNGLE_TREE, TreeFeatures.MEGA_SPRUCE, TreeFeatures.AZALEA_TREE, TreeFeatures.MANGROVE,
                TreeFeatures.CHERRY, TreeFeatures.PALE_OAK, TreeFeatures.RED_POPLAR, TreeFeatures.ORANGE_POPLAR, TreeFeatures.YELLOW_POPLAR));
    }

    /**
     * Grows these nine trees in a 3 x 3 grid 16 apart, the leaves test's, and checks each as it grows, before the next
     * can touch it: its 16 x 16 cell holds tree blocks and no vanilla log, and every tree block's faces follow the rule.
     */
    private static void treesGrowTreeBlocks(GameTestHelper helper, List<ResourceKey<Feature>> trees) {
        ServerLevel level = helper.getLevel();
        List<String> wrong = new ArrayList<>(), grown = new ArrayList<>();
        for (int t = 0; t < trees.size(); t++) {
            String name = trees.get(t).identifier().getPath();
            Feature tree = level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(trees.get(t)).value();
            BlockPos origin = helper.absolutePos(new BlockPos(8 + 16 * (t % 3), 1, 8 + 16 * (t / 3)));
            helper.assertTrue(tree.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), origin), name + " didn't grow");
            int treeBlocks = 0;
            List<String> logs = new ArrayList<>(), badFaces = new ArrayList<>();
            int x0 = 16 * (t % 3), z0 = 16 * (t / 3);
            for (BlockPos rel : BlockPos.betweenClosed(x0, 1, z0, x0 + 15, 39, z0 + 15)) {
                BlockState state = helper.getBlockState(rel);
                BlockPos pos = helper.absolutePos(rel);
                if (isTreeBlock(state)) {
                    treeBlocks++;
                    for (Direction side : Direction.values()) {
                        if (bark(state, side) != shouldBeBark(level, pos, side)) {
                            badFaces.add(rel.toShortString() + " " + side.getName() + " shows " + (bark(state, side) ? "bark" : "the cut")
                                    + " against " + level.getBlockState(pos.relative(side)).getBlock().getName().getString());
                        }
                    }
                } else if (state.is(BlockTags.LOGS)) {
                    logs.add(rel.toShortString() + " " + state.getBlock().getName().getString());
                }
            }
            if (treeBlocks == 0) wrong.add(name + " grew no tree blocks");
            if (!logs.isEmpty()) wrong.add(name + " grew " + logs.size() + " vanilla logs (" + String.join("; ", logs.subList(0, Math.min(3, logs.size()))) + ")");
            if (!badFaces.isEmpty()) wrong.add(name + " has " + badFaces.size() + " faces against the rule (" + String.join("; ", badFaces.subList(0, Math.min(3, badFaces.size()))) + ")");
            grown.add(name + " " + treeBlocks);
        }
        helper.assertTrue(wrong.isEmpty(), String.join(", ", wrong));
        VersusMod.MOD_LOGGER.info("Trees: every tree grew tree blocks ({})", String.join(", ", grown));
        helper.succeed();
    }

    /** An oak's trunk, as it grows: cut where it meets the ground and the trunk above, bark all round. */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void anOaksTrunkShowsBarkAllRound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block oakTree = treeBlock(helper, "oak");
        Feature oak = level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(TreeFeatures.OAK).value();
        helper.assertTrue(oak.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), helper.absolutePos(new BlockPos(8, 1, 8))), "the oak didn't grow");
        BlockState base = helper.getBlockState(new BlockPos(8, 1, 8));
        helper.assertTrue(base.is(oakTree) && base.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y, "the oak's trunk starts with " + base);
        helper.assertTrue(!bark(base, Direction.DOWN) && !bark(base, Direction.UP), "the base of the trunk isn't cut where it meets the ground and the trunk: " + faces(base));
        for (Direction side : Direction.Plane.HORIZONTAL) helper.assertTrue(bark(base, side), "the base of the trunk isn't bark all round: " + faces(base));
        helper.succeed();
    }

    /** Nothing changes a face once set: not a block placed against it, not a neighbouring tree block broken. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void facesStayAsTheyWereSet(GameTestHelper helper) {
        Block oakTree = treeBlock(helper, "oak");
        BlockState barkAllRound = oakTree.defaultBlockState();
        for (Direction side : Direction.values()) barkAllRound = barkAllRound.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
        BlockState cutAbove = barkAllRound.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP), false);
        helper.setBlock(2, 2, 2, barkAllRound);
        helper.setBlock(6, 2, 2, cutAbove);
        helper.setBlock(6, 3, 2, barkAllRound);
        helper.runAfterDelay(2, () -> {
            helper.setBlock(3, 2, 2, Blocks.STONE.defaultBlockState());
            helper.setBlock(6, 3, 2, Blocks.AIR.defaultBlockState());
        });
        BlockState untouched = barkAllRound, cut = cutAbove;
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).equals(untouched), "a block placed against bark cut it: " + faces(helper.getBlockState(new BlockPos(2, 2, 2))));
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 2, 2)).equals(cut), "breaking the tree block above covered the cut: " + faces(helper.getBlockState(new BlockPos(6, 2, 2))));
            helper.succeed();
        });
    }

    /** Placed in creative, a tree block takes its axis from the face clicked and sets its faces from its neighbours. */
    @GameTest(structure = ROOM)
    public void aPlacedTreeBlockSetsItsFacesFromItsNeighbours(GameTestHelper helper) {
        Block oakTree = treeBlock(helper, "oak");
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.setBlock(2, 1, 2, Blocks.STONE.defaultBlockState());
        helper.setBlock(5, 2, 2, Blocks.OAK_LOG.defaultBlockState());
        BlockState onStone = place(helper, player, oakTree, new BlockPos(2, 1, 2), Direction.UP);
        helper.assertTrue(onStone != null && onStone.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y, "placed on top of stone: " + onStone);
        helper.assertTrue(!bark(onStone, Direction.DOWN), "the face on the stone isn't cut: " + faces(onStone));
        for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            helper.assertTrue(bark(onStone, side), "a face against air isn't bark: " + faces(onStone));
        }
        BlockState onLog = place(helper, player, oakTree, new BlockPos(5, 2, 2), Direction.WEST);
        helper.assertTrue(onLog != null && onLog.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "placed on the side of a log: " + onLog);
        helper.assertTrue(!bark(onLog, Direction.EAST) && bark(onLog, Direction.WEST) && bark(onLog, Direction.UP), "against the log: " + faces(onLog));
        helper.succeed();
    }

    /** What a creative player placing this block against that face of that block would place. */
    private static BlockState place(GameTestHelper helper, Player player, Block block, BlockPos against, Direction face) {
        BlockPos clicked = helper.absolutePos(against);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block.asItem()),
                new BlockHitResult(Vec3.atCenterOf(clicked).relative(face, 0.5), face, clicked, false));
        return block.getStateForPlacement(context);
    }

    /** Breaking a tree block gives its wood's vanilla log, whatever breaks it. */
    @GameTest(structure = ROOM)
    public void treeBlocksDropTheirWoodsLog(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        ItemStack silkTouch = new ItemStack(Items.DIAMOND_AXE);
        silkTouch.enchant(EnchantRegistryHelper.getRegistryEntry(level, Enchantments.SILK_TOUCH), 1);
        for (Wood wood : WOODS) {
            BlockState tree = treeBlock(helper, wood.name()).defaultBlockState();
            for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_AXE), silkTouch)) {
                List<ItemStack> drops = Block.getDrops(tree, level, pos, null, null, tool);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(wood.log().asItem()) && drops.getFirst().getCount() == 1,
                        wood.name() + " tree broken with " + (tool.isEmpty() ? "a bare hand" : tool) + " dropped " + drops);
            }
        }
        helper.succeed();
    }

    /**
     * To the game a tree block is its wood's log: in its wood's tag and so in the logs that burn and all logs, mined
     * with an axe, with an item for creative, and a cocoa pod stays on a jungle one.
     */
    @GameTest(structure = ROOM)
    public void treeBlocksAreLogsToTheGame(GameTestHelper helper) {
        for (Wood wood : WOODS) {
            Block tree = treeBlock(helper, wood.name());
            BlockState state = tree.defaultBlockState();
            TagKey<Block> own = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(wood.name() + "_logs"));
            helper.assertTrue(state.is(own), wood.name() + " tree isn't in " + own);
            helper.assertTrue(state.is(BlockTags.LOGS_THAT_BURN) && state.is(BlockTags.LOGS), wood.name() + " tree isn't a log that burns");
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), wood.name() + " tree isn't mined with an axe");
            Item item = tree.asItem();
            helper.assertTrue(item instanceof BlockItem, wood.name() + " tree has no item");
        }
        Block jungleTree = treeBlock(helper, "jungle");
        helper.setBlock(2, 2, 2, jungleTree.defaultBlockState());
        BlockState cocoa = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, Direction.WEST);
        helper.assertTrue(cocoa.canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 2))), "a cocoa pod can't grow on a jungle tree block");
        helper.succeed();
    }
}
