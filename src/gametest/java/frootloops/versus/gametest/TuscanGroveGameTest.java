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
}
