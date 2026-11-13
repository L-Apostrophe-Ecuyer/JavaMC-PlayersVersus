package frootloops.versus.gametest;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.trees.TreeBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * River banks: what grows and lies at the water's edge in the temperate biomes (reeds, tall grass and ferns, willows, muddy
 * banks) stands only within {@link #REACH} blocks of water, and each of them shows up along a pond's edge.
 */
public class RiverbanksGameTest {

    // 48 x 40 x 48, a grass floor, with sky access so that the height maps find the ground and not the barrier ceiling
    private static final String GROVE = "players-versus-gametest:leaves_grove";
    /** The land: from FROM to TO along x and z, two blocks over the grove's floor (dirt, then grass); a pond in its middle. */
    private static final int FROM = 2, TO = 45, MIDDLE = 24, POND = 7, HEIGHT = 24, RUNS = 12;
    /** How far from the water, along x or z, a riverbank feature may put anything. */
    private static final int REACH = 4;

    /**
     * Levels the land two blocks over the grove's floor and digs the pond into it: water two deep, as high as the land, its
     * bottom the floor. Clears everything above. Returns the land's top y (the grass's, the water's surface).
     */
    private static int land(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int floor = helper.absolutePos(BlockPos.ZERO).getY();
        for (int x = FROM; x <= TO; x++) {
            for (int z = FROM; z <= TO; z++) {
                boolean pond = inPond(x, z);
                BlockPos column = helper.absolutePos(new BlockPos(x, 0, z));
                level.setBlock(column, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(column.above(1), (pond ? Blocks.WATER : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(column.above(2), (pond ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 3; y < HEIGHT; y++) {
                    if (!level.getBlockState(column.above(y)).isAir()) level.setBlock(column.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return floor + 2;
    }

    private static boolean inPond(int x, int z) {
        return (x - MIDDLE) * (x - MIDDLE) + (z - MIDDLE) * (z - MIDDLE) <= POND * POND;
    }

    /** Whether the column (x, z) of the grove has the pond's water within {@link #REACH} blocks along x and z. */
    private static boolean withinReach(int x, int z) {
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                if (inPond(x + dx, z + dz)) return true;
            }
        }
        return false;
    }

    /**
     * Runs the placed feature {@link #RUNS} times from the corner of every chunk the land touches, then finds the blocks it
     * made (those that {@code made} matches, over the land and in its top two layers), checking each is within reach of the
     * water. Returns where they are, relative to the grove.
     */
    private static List<BlockPos> place(GameTestHelper helper, String name, Predicate<BlockState> made) {
        return place(helper, name, PlacedFeatures.withoutBiomeCheck(helper, name, 4), made);
    }

    private static List<BlockPos> place(GameTestHelper helper, String name, PlacedFeature feature, Predicate<BlockState> made) {
        ServerLevel level = helper.getLevel();
        int top = land(helper);
        BlockPos from = helper.absolutePos(new BlockPos(FROM, 0, FROM)), to = helper.absolutePos(new BlockPos(TO, 0, TO));
        for (int run = 0; run < RUNS; run++) {
            for (int cx = Math.min(from.getX(), to.getX()) & ~15; cx <= Math.max(from.getX(), to.getX()); cx += 16) {
                for (int cz = Math.min(from.getZ(), to.getZ()) & ~15; cz <= Math.max(from.getZ(), to.getZ()); cz += 16) {
                    feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(run * 7919L + cx * 31L + cz), new BlockPos(cx, top + 1, cz));
                }
            }
        }
        List<BlockPos> found = new ArrayList<>();
        int floor = top - 2;
        for (int x = FROM; x <= TO; x++) {
            for (int z = FROM; z <= TO; z++) {
                for (int y = 1; y < HEIGHT; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (!made.test(helper.getBlockState(at))) continue;
                    helper.assertTrue(withinReach(x, z), name + " put " + helper.getBlockState(at).getBlock().getName().getString() + " at " + x + ", " + y + ", " + z
                            + ", further than " + REACH + " blocks from the water");
                    found.add(at);
                }
            }
        }
        helper.assertTrue(floor == helper.absolutePos(BlockPos.ZERO).getY(), "the land's floor moved");
        return found;
    }

    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void reedsGrowAtTheWatersEdge(GameTestHelper helper) {
        List<BlockPos> reeds = place(helper, "riverbank_reeds", state -> state.is(Blocks.SUGAR_CANE));
        helper.assertTrue(reeds.size() >= 6, "only " + reeds.size() + " sugar cane by the pond");
        helper.succeed();
    }

    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void tallGrassFernsAndBushesGrowNearTheWater(GameTestHelper helper) {
        List<BlockPos> plants = place(helper, "riverbank_plants", state -> state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.BUSH));
        long grass = plants.stream().filter(at -> helper.getBlockState(at).is(Blocks.TALL_GRASS)).count();
        long ferns = plants.stream().filter(at -> helper.getBlockState(at).is(Blocks.LARGE_FERN)).count();
        helper.assertTrue(plants.size() >= 12, "only " + plants.size() + " plants by the pond");
        helper.assertTrue(grass > 0 && ferns > 0, grass + " tall grass and " + ferns + " large ferns: both grow by the water");
        helper.succeed();
    }

    /** Willows (swamp oaks, vines on their leaves) stand on the bank, not in the water. Their trunks are the mod's tree blocks. */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void willowsStandOnTheBank(GameTestHelper helper) {
        List<BlockPos> logs = place(helper, "riverbank_willows", state -> state.getBlock() instanceof TreeBlock);
        List<BlockPos> trunks = logs.stream().filter(at -> !(helper.getBlockState(at.below()).getBlock() instanceof TreeBlock)).toList();
        helper.assertTrue(!trunks.isEmpty(), "no willow by the pond");
        for (BlockPos trunk : trunks) {
            helper.assertTrue(trunk.getY() == 3, "a willow's trunk starts at " + trunk.toShortString() + ", not on the bank (3 over the floor)");
            helper.assertTrue(!inPond(trunk.getX(), trunk.getZ()), "a willow in the pond at " + trunk.toShortString());
        }
        helper.succeed();
    }

    /**
     * Out of the Players Versus world type (the test world is another) the river banks place nothing, vanilla's world types
     * keeping vanilla's worldgen: the features as the data has them, but for their biome check.
     */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void theBanksAreThePlayersVersusWorldsOnly(GameTestHelper helper) {
        Registry<PlacedFeature> features = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (String name : List.of("riverbank_mud", "riverbank_reeds", "riverbank_plants", "riverbank_willows")) {
            PlacedFeature data = features.getValue(Identifier.parse("players-versus:" + name));
            helper.assertTrue(data != null, "players-versus:" + name + " is not a placed feature");
            PlacedFeature feature = new PlacedFeature(data.feature(), data.placement().stream().filter(modifier -> !(modifier instanceof BiomeFilter)).toList());
            List<BlockPos> made = place(helper, name, feature, state -> state.is(Blocks.SUGAR_CANE) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)
                    || state.is(Blocks.BUSH) || state.getBlock() instanceof TreeBlock || state.is(CustomBlocks.GRAY_MUD) || state.is(CustomBlocks.BROWN_MUD));
            helper.assertTrue(made.isEmpty(), name + " placed " + made.size() + " blocks in a world that isn't a Players Versus one");
        }
        helper.succeed();
    }

    /** Muddy banks: the mod's gray mud at the water, brown mud behind it, in the bank's top two layers. */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void theBanksAreMuddy(GameTestHelper helper) {
        List<BlockPos> mud = place(helper, "riverbank_mud", state -> state.is(CustomBlocks.GRAY_MUD) || state.is(CustomBlocks.BROWN_MUD));
        helper.assertTrue(mud.size() >= 8, "only " + mud.size() + " mud by the pond");
        for (BlockPos at : mud) helper.assertTrue(at.getY() == 1 || at.getY() == 2, "mud at " + at.toShortString() + ", out of the bank's top two layers");
        helper.assertTrue(mud.stream().anyMatch(at -> helper.getBlockState(at).is(CustomBlocks.GRAY_MUD)), "no gray mud at the water");
        helper.succeed();
    }
}
