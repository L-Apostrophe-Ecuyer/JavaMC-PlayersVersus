package frootloops.versus.gametest;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.leaves.LeafReach;
import frootloops.versus.mod.environment.blocks.trees.TreeBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Tuscan grove's cypress: a tall, narrow column of spruce leaves on a straight trunk, whatever the seed (a foliage placer
 * that adds a random radius, as the pine's does, makes two in three of them cones as wide as spruces).
 */
public class TuscanGroveGameTest {

    // 48 x 40 x 48, a grass floor, with sky access
    private static final String GROVE = "players-versus-gametest:leaves_grove";
    private static final int SEEDS = 24, FROM = 14, TO = 34, HEIGHT = 34;

    /** The cleared space around the grove's middle (24, 24): air over its grass floor. */
    private static void clear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = FROM; x <= TO; x++) {
            for (int z = FROM; z <= TO; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
                    BlockState want = y == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (!level.getBlockState(pos).equals(want)) level.setBlock(pos, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /**
     * A cypress is a fuzzy pear on a spruce trunk, as the owner's picture: its crown wider below than above, a spire of
     * single leaves over the trunk's top, no leaf more than two blocks from the trunk (but some two blocks out: fuzz), of
     * spruce leaves with cypress leaves for highlights, more of them up high.
     */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void aCypressIsAFuzzyPear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature cypress = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.parse("players-versus:cypress"));
        helper.assertTrue(cypress != null, "players-versus:cypress is not a feature");
        BlockPos origin = helper.absolutePos(new BlockPos(24, 1, 24));
        int fuzzy = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            clear(helper);
            helper.assertTrue(cypress.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin), "the cypress didn't grow with seed " + seed);
            int[] perRow = new int[HEIGHT], highlights = new int[HEIGHT];
            int trunk = 0, widest = 0, outermost = 0, bottom = HEIGHT, top = 0;
            for (int x = FROM; x <= TO; x++) {
                for (int z = FROM; z <= TO; z++) {
                    for (int y = 1; y < HEIGHT; y++) {
                        BlockPos rel = new BlockPos(x, y, z);
                        BlockState state = helper.getBlockState(rel);
                        if (state.is(BlockTags.LEAVES)) {
                            helper.assertTrue(state.is(Blocks.SPRUCE_LEAVES) || state.is(CustomBlocks.CYPRESS_LEAVES), "seed " + seed + ": " + state + " in a cypress");
                            perRow[y]++;
                            if (state.is(CustomBlocks.CYPRESS_LEAVES)) highlights[y]++;
                            int away = Math.max(Math.abs(x - 24), Math.abs(z - 24));
                            widest = Math.max(widest, away);
                            if (away == 2) outermost++;
                            bottom = Math.min(bottom, y);
                            top = Math.max(top, y);
                        } else if (state.getBlock() instanceof TreeBlock) {
                            trunk++;
                            helper.assertTrue(x == 24 && z == 24, "seed " + seed + ": a trunk block off the trunk's column, at " + rel.toShortString());
                        }
                    }
                }
            }
            helper.assertTrue(trunk >= 12, "seed " + seed + ": a trunk of only " + trunk + " blocks");
            helper.assertTrue(widest <= 2, "seed " + seed + ": leaves " + widest + " blocks from the trunk");
            if (outermost > 0) fuzzy++;
            // A spire: the top two rows single leaves over the trunk, the crown's top over the trunk's
            helper.assertTrue(top > trunk && perRow[top] == 1 && perRow[top - 1] == 1 && helper.getBlockState(new BlockPos(24, top, 24)).is(BlockTags.LEAVES),
                    "seed " + seed + ": no spire of single leaves on top (rows " + perRow[top - 1] + ", " + perRow[top] + ")");
            // A pear: more leaves a row in the crown's lower half than in its upper half
            int middle = (bottom + top) / 2, lower = 0, lowerRows = 0, upper = 0, upperRows = 0, lowerLight = 0, upperLight = 0;
            for (int y = bottom; y <= top; y++) {
                if (y <= middle) { lower += perRow[y]; lowerRows++; lowerLight += highlights[y]; }
                else { upper += perRow[y]; upperRows++; upperLight += highlights[y]; }
            }
            helper.assertTrue((double) lower / lowerRows > 1.3 * upper / upperRows,
                    "seed " + seed + ": " + lower + " leaves in the lower half's " + lowerRows + " rows against " + upper + " in the upper half's " + upperRows + ": no pear");
            helper.assertTrue(lowerLight + upperLight > 0 && lowerLight + upperLight < lower + upper, "seed " + seed + ": not a mix of spruce and cypress leaves");
            helper.assertTrue((double) upperLight / upper > (double) lowerLight / lower,
                    "seed " + seed + ": cypress leaves " + upperLight + " of " + upper + " up high, " + lowerLight + " of " + lower + " below");
        }
        helper.assertTrue(fuzzy >= SEEDS / 2, "only " + fuzzy + " of " + SEEDS + " cypresses have a leaf sticking out two blocks");
        clear(helper);
        helper.succeed();
    }

    /**
     * A field of flowers is a mix of flower kinds (the meadow's own provider picks one to three kinds for a whole region of a
     * thousand blocks, and a region may have no flower at all), and a field of dry grass is dry grass: the placed features run over
     * the grove's floor, four patches of the one and two of the other.
     */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void theFieldsGrowAMixOfFlowersAndDryGrass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos middle = helper.absolutePos(new BlockPos(24, 1, 24));
        int x0 = middle.getX() & ~15, z0 = middle.getZ() & ~15, floor = middle.getY() - 1;

        clear(helper);
        PlacedFeature flowers = PlacedFeatures.withoutBiomeCheck(helper, "tuscan_flower_field", 4);
        for (int run = 0; run < 6; run++) flowers.place(level, level.getChunkSource().getGenerator(), RandomSource.create(run), new BlockPos(x0, floor + 1, z0));
        Set<Block> kinds = new HashSet<>();
        int flower = 0;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                for (int y = floor + 1; y <= floor + 3; y++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.is(BlockTags.SMALL_FLOWERS)) {
                        flower++;
                        kinds.add(state.getBlock());
                    }
                }
            }
        }
        helper.assertTrue(flower >= 40, "only " + flower + " flowers in the field");
        helper.assertTrue(kinds.size() >= 4, "a field of " + kinds.size() + " kinds of flower: " + kinds);

        clear(helper);
        PlacedFeature dry = PlacedFeatures.withoutBiomeCheck(helper, "tuscan_dry_grass", 2);
        for (int run = 0; run < 6; run++) dry.place(level, level.getChunkSource().getGenerator(), RandomSource.create(run), new BlockPos(x0, floor + 1, z0));
        int dryGrass = 0;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                BlockState state = level.getBlockState(new BlockPos(x, floor + 1, z));
                if (state.is(Blocks.SHORT_DRY_GRASS) || state.is(Blocks.TALL_DRY_GRASS)) dryGrass++;
            }
        }
        helper.assertTrue(dryGrass >= 20, "only " + dryGrass + " dry grass in the field");
        clear(helper);
        helper.succeed();
    }

    /**
     * Cypress leaves are spruce leaves in lighter colours: leaves, alive by spruce logs (not oak); shears take them,
     * otherwise they drop as spruce leaves (spruce saplings and sticks, never themselves).
     */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void cypressLeavesAreSpruceLeavesInLighterColours(GameTestHelper helper) {
        Block cypress = CustomBlocks.CYPRESS_LEAVES;
        BlockState state = cypress.defaultBlockState();
        helper.assertTrue(state.is(BlockTags.LEAVES), "cypress leaves aren't leaves");
        helper.assertTrue(LeafReach.isOwnLog(cypress, Blocks.SPRUCE_LOG.defaultBlockState()), "a spruce log doesn't keep cypress leaves alive");
        helper.assertTrue(!LeafReach.isOwnLog(cypress, Blocks.OAK_LOG.defaultBlockState()), "an oak log keeps cypress leaves alive");
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(5, 5, 5));
        List<ItemStack> sheared = Block.getDrops(state, level, at, null, null, new ItemStack(Items.SHEARS));
        helper.assertTrue(sheared.size() == 1 && sheared.getFirst().is(cypress.asItem()), "shears took " + sheared);
        Set<Item> dropped = new HashSet<>();
        for (int i = 0; i < 400; i++) Block.getDrops(state, level, at, null, null, ItemStack.EMPTY).forEach(stack -> dropped.add(stack.getItem()));
        helper.assertTrue(dropped.contains(Items.SPRUCE_SAPLING) && dropped.contains(Items.STICK) && !dropped.contains(cypress.asItem()),
                "cypress leaves dropped " + dropped + " in 400 breaks");
        helper.succeed();
    }
}
