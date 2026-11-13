package frootloops.versus.gametest;

import frootloops.versus.mod.environment.blocks.trees.TreeBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;
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

    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void aCypressIsANarrowColumn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature cypress = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.parse("players-versus:cypress"));
        helper.assertTrue(cypress != null, "players-versus:cypress is not a feature");
        BlockPos origin = helper.absolutePos(new BlockPos(24, 1, 24));
        for (int seed = 0; seed < SEEDS; seed++) {
            clear(helper);
            helper.assertTrue(cypress.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin), "the cypress didn't grow with seed " + seed);
            int leaves = 0, trunk = 0, widest = 0, top = 0;
            for (int x = FROM; x <= TO; x++) {
                for (int z = FROM; z <= TO; z++) {
                    for (int y = 1; y < HEIGHT; y++) {
                        BlockPos rel = new BlockPos(x, y, z);
                        BlockState state = helper.getBlockState(rel);
                        if (state.is(BlockTags.LEAVES)) {
                            leaves++;
                            widest = Math.max(widest, Math.max(Math.abs(x - 24), Math.abs(z - 24)));
                            top = Math.max(top, y);
                        } else if (state.getBlock() instanceof TreeBlock) {
                            trunk++;
                            helper.assertTrue(x == 24 && z == 24, "seed " + seed + ": a trunk block off the trunk's column, at " + rel.toShortString());
                        }
                    }
                }
            }
            // A column three blocks across (the leaves' plus), from near the ground to over the trunk's top: tall and not a bare pole
            helper.assertTrue(widest == 1, "seed " + seed + ": leaves " + widest + " blocks from the trunk, not 1");
            helper.assertTrue(trunk >= 12, "seed " + seed + ": a trunk of only " + trunk + " blocks");
            helper.assertTrue(top >= trunk, "seed " + seed + ": the crown's top, " + top + ", is under the trunk's");
            helper.assertTrue(leaves >= 30, "seed " + seed + ": only " + leaves + " leaves");
        }
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
}
