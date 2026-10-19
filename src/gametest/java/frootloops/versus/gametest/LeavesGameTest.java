package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.leaves.FallenLeavesBlock;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import java.util.List;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Leaves as husks (which don't burn) find them on the game test server: pushing through, climbing, sinking, falling onto
 * and through them, and the litter they shed. Mobs move on the server, so this is the movement a player's client
 * simulates for itself through the same code (LeafMovement).
 */
public class LeavesGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room";   // 14 x 8 x 8
    private static final String TOWER = "players-versus-gametest:leaves_tower"; // 12 x 24 x 6

    /** Persistent, so they don't decay while the test runs. */
    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) helper.setBlock(x, y, z, state);
    }

    private static void floor(GameTestHelper helper, int sizeX, int sizeZ) {
        fill(helper, 0, 0, 0, sizeX - 1, 0, sizeZ - 1, Blocks.STONE.defaultBlockState());
    }

    private static Mob husk(GameTestHelper helper, double x, double y, double z) {
        return helper.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(x, y, z));
    }

    private static double damageTaken(LivingEntity entity) {
        return entity.getMaxHealth() - entity.getHealth();
    }

    private static double relativeY(GameTestHelper helper, LivingEntity entity) {
        return helper.relativeVec(entity.position()).y;
    }

    /** A hedge two blocks high slows a fast walk well below the open ground's, but the husk keeps going through it. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aHedgeSlowsAWalkButDoesntStopIt(GameTestHelper helper) {
        floor(helper, 14, 8);
        fill(helper, 4, 1, 4, 12, 2, 6, leaves(Blocks.OAK_LEAVES));
        Mob open = husk(helper, 1.5, 1.0, 1.5), hedged = husk(helper, 1.5, 1.0, 5.5);
        double startX = helper.relativeVec(hedged.position()).x;
        double[] highest = {0.0};
        helper.onEachTick(() -> {
            for (Mob husk : new Mob[]{open, hedged}) {
                Vec3 target = helper.absoluteVec(new Vec3(13.5, 1.0, helper.relativeVec(husk.position()).z));
                husk.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.5);
            }
            highest[0] = Math.max(highest[0], relativeY(helper, hedged));
        });
        helper.runAfterDelay(40, () -> {
            double walkedOpen = helper.relativeVec(open.position()).x - startX;
            double walkedHedged = helper.relativeVec(hedged.position()).x - startX;
            VersusMod.MOD_LOGGER.info("Leaves: in 40 ticks a husk walked {} in the open, {} through a hedge (highest {})", walkedOpen, walkedHedged, highest[0]);
            helper.assertTrue(walkedOpen > 5.0, "the husk on open ground walked " + walkedOpen);
            helper.assertTrue(walkedHedged < 0.85 * walkedOpen, "through the hedge " + walkedHedged + ", in the open " + walkedOpen);
            helper.assertTrue(walkedHedged > 0.3 * walkedOpen, "the hedge nearly stopped it: " + walkedHedged + " against " + walkedOpen);
            helper.assertTrue(highest[0] < 1.6, "walking through the hedge, it went up to " + highest[0]);
            helper.succeed();
        });
    }

    /** Holding jump with leaves at its feet climbs to the top of them; leaves just above its head can't be climbed. */
    @GameTest(structure = ROOM, maxTicks = 120)
    public void leavesAtTheFeetClimbLeavesAboveTheHeadDont(GameTestHelper helper) {
        floor(helper, 14, 8);
        fill(helper, 1, 1, 1, 3, 4, 3, leaves(Blocks.OAK_LEAVES));
        fill(helper, 8, 3, 1, 10, 4, 3, leaves(Blocks.OAK_LEAVES));
        Mob climber = husk(helper, 2.5, 1.0, 2.5), under = husk(helper, 9.5, 1.0, 2.5);
        double[] highestUnder = {0.0};
        helper.onEachTick(() -> {
            climber.getJumpControl().jump();
            under.getJumpControl().jump();
            highestUnder[0] = Math.max(highestUnder[0], relativeY(helper, under));
        });
        helper.runAfterDelay(90, () -> {
            VersusMod.MOD_LOGGER.info("Leaves: climbing got a husk to {}, jumping under leaves to {}", relativeY(helper, climber), highestUnder[0]);
            helper.assertTrue(relativeY(helper, climber) > 4.5, "climbing the leaves it stood in, it got to " + relativeY(helper, climber));
            helper.assertTrue(highestUnder[0] < 2.6, "jumping under leaves above its head, it got to " + highestUnder[0]);
            helper.succeed();
        });
    }

    /** Falling 15 blocks onto stone hurts; through a three-layer canopy on the way down, hardly at all. */
    @GameTest(structure = TOWER, maxTicks = 100)
    public void aCanopyBreaksAFall(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 5, 1, 3, 7, 3, leaves(Blocks.OAK_LEAVES));
        Mob canopy = husk(helper, 2.5, 16.0, 2.5), bare = husk(helper, 8.5, 16.0, 2.5);
        helper.runAfterDelay(70, () -> {
            double hurtBare = damageTaken(bare), hurtCanopy = damageTaken(canopy);
            VersusMod.MOD_LOGGER.info("Leaves: a 15-block fall hurt a husk {} onto stone, {} through three layers", hurtBare, hurtCanopy);
            helper.assertTrue(hurtBare >= 9.0, "a 15-block fall onto stone did only " + hurtBare);
            helper.assertTrue(hurtCanopy <= 3.0 && hurtCanopy < hurtBare / 2.0, "through the canopy " + hurtCanopy + ", onto stone " + hurtBare);
            helper.succeed();
        });
    }

    /** One layer of leaves slows a fall a bit, no more: it hurts less than onto stone, but still hurts. */
    @GameTest(structure = TOWER, maxTicks = 100)
    public void oneLayerOnlySlowsAFallABit(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 5, 1, 3, 5, 3, leaves(Blocks.OAK_LEAVES));
        Mob layer = husk(helper, 2.5, 16.0, 2.5), bare = husk(helper, 8.5, 16.0, 2.5);
        helper.runAfterDelay(70, () -> {
            double hurtBare = damageTaken(bare), hurtLayer = damageTaken(layer);
            VersusMod.MOD_LOGGER.info("Leaves: a 15-block fall hurt a husk {} onto stone, {} through one layer", hurtBare, hurtLayer);
            helper.assertTrue(hurtLayer < hurtBare, "one layer of leaves didn't soften the fall: " + hurtLayer + " against " + hurtBare);
            helper.assertTrue(hurtLayer >= 0.25 * hurtBare, "one layer of leaves took almost all of the fall: " + hurtLayer + " against " + hurtBare);
            helper.succeed();
        });
    }

    /** A short drop lands on top of leaves; a longer one breaks through them to the ground. */
    @GameTest(structure = TOWER, maxTicks = 80)
    public void aShortDropLandsOnTopALongOneBreaksThrough(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 3, 1, 3, leaves(Blocks.OAK_LEAVES));
        fill(helper, 7, 1, 1, 9, 1, 3, leaves(Blocks.OAK_LEAVES));
        Mob dropped = husk(helper, 2.5, 3.5, 2.5), thrown = husk(helper, 8.5, 7.0, 2.5);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(relativeY(helper, dropped) > 1.99, "a 1.5-block drop sank to " + relativeY(helper, dropped));
            helper.assertTrue(dropped.onGround(), "a 1.5-block drop isn't standing on the leaf");
            helper.assertTrue(relativeY(helper, thrown) < 1.5, "a 5-block drop stayed at " + relativeY(helper, thrown));
            helper.succeed();
        });
    }

    /** Animals don't go through leaves: a cow lands on top of them, even from higher up. */
    @GameTest(structure = TOWER, maxTicks = 80)
    public void animalsStandOnLeaves(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 3, 1, 3, leaves(Blocks.OAK_LEAVES));
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new Vec3(2.5, 7.0, 2.5));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(relativeY(helper, cow) > 1.99, "the cow went into the leaves, down to " + relativeY(helper, cow));
            helper.succeed();
        });
    }

    /** Sneaking on top of leaves sinks down through them, slowly and unhurt. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void sneakingSinksThroughLeaves(GameTestHelper helper) {
        floor(helper, 14, 8);
        fill(helper, 1, 1, 1, 3, 3, 3, leaves(Blocks.OAK_LEAVES));
        Mob husk = husk(helper, 2.5, 4.0, 2.5);
        double[] fastest = {0.0};
        helper.onEachTick(() -> {
            husk.setShiftKeyDown(true);
            fastest[0] = Math.max(fastest[0], husk.yo - husk.getY());
        });
        helper.runAfterDelay(70, () -> {
            VersusMod.MOD_LOGGER.info("Leaves: sneaking sank a husk to {}, at most {} a tick", relativeY(helper, husk), fastest[0]);
            helper.assertTrue(relativeY(helper, husk) < 1.5, "sneaking, it only sank to " + relativeY(helper, husk));
            helper.assertTrue(fastest[0] < 0.2, "it sank as fast as " + fastest[0] + " blocks a tick");
            helper.assertTrue(damageTaken(husk) == 0.0, "sinking through leaves hurt it");
            helper.succeed();
        });
    }

    /** On the server a player's leaves are always open (its client decides); a mob above them stands on them. */
    @GameTest(structure = ROOM)
    public void theServerLetsPlayersThrough(GameTestHelper helper) {
        floor(helper, 14, 8);
        helper.setBlock(2, 1, 2, leaves(Blocks.OAK_LEAVES));
        BlockPos leaf = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 onTop = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(onTop.x, onTop.y, onTop.z);
        Mob husk = husk(helper, 2.5, 2.0, 2.5);
        helper.assertFalse(LeafMovement.isSolid(leaf, CollisionContext.of(player)), "the server held a player up on a leaf");
        helper.assertTrue(LeafMovement.isSolid(leaf, CollisionContext.of(husk)), "a husk above a leaf isn't held up");
        helper.succeed();
    }

    /** Litter falls through the leaves below to the ground, piles up to four, never onto water or into a plant. */
    @GameTest(structure = TOWER)
    public void litterFallsThroughTheCanopyAndPilesUp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 1, Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(1, 6, 1, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(1, 7, 1, leaves(Blocks.OAK_LEAVES));
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(LeafLitter.shed(level, helper.absolutePos(new BlockPos(1, 7, 1)), leaves(Blocks.OAK_LEAVES), level.getRandom()),
                    "segment " + (i + 1) + " didn't land");
        }
        helper.assertFalse(LeafLitter.shed(level, helper.absolutePos(new BlockPos(1, 7, 1)), leaves(Blocks.OAK_LEAVES), level.getRandom()),
                "a fifth segment landed on a full pile");
        helper.assertBlockPresent(Blocks.LEAF_LITTER, new BlockPos(1, 2, 1));
        helper.assertBlockProperty(new BlockPos(1, 2, 1), SegmentableBlock.AMOUNT, 4);

        helper.assertTrue(LeafLitter.shed(level, helper.absolutePos(new BlockPos(3, 6, 1)), leaves(Blocks.CHERRY_LEAVES), level.getRandom()),
                "cherry leaves shed nothing");
        helper.assertBlockPresent(Blocks.PINK_PETALS, new BlockPos(3, 2, 1));
        helper.assertFalse(LeafLitter.shed(level, helper.absolutePos(new BlockPos(5, 6, 1)), leaves(Blocks.SPRUCE_LEAVES), level.getRandom()),
                "spruce leaves shed litter");

        helper.setBlock(7, 1, 1, Blocks.WATER.defaultBlockState());
        helper.assertFalse(LeafLitter.shed(level, helper.absolutePos(new BlockPos(7, 6, 1)), leaves(Blocks.OAK_LEAVES), level.getRandom()),
                "litter landed on water");
        helper.setBlock(9, 2, 1, Blocks.SHORT_GRASS.defaultBlockState());
        helper.assertFalse(LeafLitter.shed(level, helper.absolutePos(new BlockPos(9, 6, 1)), leaves(Blocks.OAK_LEAVES), level.getRandom()),
                "litter replaced short grass");
        helper.succeed();
    }

    /** Dying leaves leave litter under them by chance: a whole layer of them leaves some. */
    @GameTest(structure = TOWER)
    public void decayingLeavesLeaveLitter(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 4, Blocks.GRASS_BLOCK.defaultBlockState());
        BlockState dying = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 7);
        fill(helper, 1, 6, 1, 10, 6, 4, dying);
        for (int x = 1; x <= 10; x++) for (int z = 1; z <= 4; z++) helper.randomTick(new BlockPos(x, 6, z));
        int segments = 0;
        for (int x = 1; x <= 10; x++) {
            for (int z = 1; z <= 4; z++) {
                helper.assertBlockNotPresent(Blocks.OAK_LEAVES, new BlockPos(x, 6, z));
                BlockState ground = helper.getBlockState(new BlockPos(x, 2, z));
                if (ground.is(Blocks.LEAF_LITTER)) segments += ground.getValue(SegmentableBlock.AMOUNT);
            }
        }
        VersusMod.MOD_LOGGER.info("Leaves: 40 decaying leaves left {} segments of litter", segments);
        helper.assertTrue(segments > 0, "40 decaying leaves left no litter");
        helper.assertTrue(segments < 40, "every decaying leaf left litter");
        helper.succeed();
    }

    /**
     * Pushing fast through natural leaves knocks litter loose; keeping still in them doesn't. As the server measures it:
     * a mob by how far it moved this tick, a player by the movement its client sent, its old position having already
     * caught up with it.
     */
    @GameTest(structure = TOWER)
    public void movingThroughLeavesKnocksLitterLoose(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 4, Blocks.GRASS_BLOCK.defaultBlockState());

        Mob husk = husk(helper, 2.5, 6.0, 2.5);
        husk.setOldPosAndRot();
        int stillHusk = litterAfterAWhileIn(helper, 2, husk);
        husk.setPos(husk.getX(), husk.getY() - 1.5, husk.getZ());
        int fallingHusk = litterAfterAWhileIn(helper, 4, husk);

        ServerPlayer player = FakePlayer.get(helper.getLevel());
        player.setOldPosAndRot();
        player.setKnownMovement(Vec3.ZERO);
        int stillPlayer = litterAfterAWhileIn(helper, 6, player);
        player.setKnownMovement(new Vec3(0.0, -1.5, 0.0));
        int fallingPlayer = litterAfterAWhileIn(helper, 8, player);
        player.setKnownMovement(Vec3.ZERO);

        helper.assertTrue(stillHusk == 0, "a still husk knocked down " + stillHusk + " segments of litter");
        helper.assertTrue(fallingHusk > 0, "a husk falling through leaves knocked down no litter");
        helper.assertTrue(stillPlayer == 0, "a still player knocked down " + stillPlayer + " segments of litter");
        helper.assertTrue(fallingPlayer > 0, "a player falling through leaves knocked down no litter");
        helper.succeed();
    }

    /** The segments of litter under natural oak leaves at (x, 6, 2) after a hundred ticks of this entity inside them. */
    private static int litterAfterAWhileIn(GameTestHelper helper, int x, Entity entity) {
        BlockState natural = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
        BlockPos leaves = helper.absolutePos(new BlockPos(x, 6, 2));
        for (int i = 0; i < 100; i++) LeafLitter.disturb(helper.getLevel(), leaves, natural, entity);
        BlockState ground = helper.getBlockState(new BlockPos(x, 2, 2));
        return ground.is(Blocks.LEAF_LITTER) ? ground.getValue(SegmentableBlock.AMOUNT) : 0;
    }

    /** A pile of fallen leaves facing east: natural ones brown, a player's never do. */
    private static BlockState fallenLeaves(int segments, boolean persistent) {
        return CustomBlocks.FALLEN_LEAVES.defaultBlockState().setValue(SegmentableBlock.AMOUNT, segments)
                .setValue(LeafLitterBlock.FACING, Direction.EAST).setValue(FallenLeavesBlock.PERSISTENT, persistent);
    }

    /** Natural fallen leaves brown into leaf litter, keeping their segments and facing; placed ones never do. */
    @GameTest(structure = TOWER)
    public void fallenLeavesBrownWithTime(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 4, Blocks.GRASS_BLOCK.defaultBlockState());
        BlockPos natural = new BlockPos(2, 2, 2), placed = new BlockPos(4, 2, 2);
        helper.setBlock(natural, fallenLeaves(3, false));
        helper.setBlock(placed, fallenLeaves(3, true));
        for (int i = 0; i < 300; i++) {
            helper.randomTick(natural);
            helper.randomTick(placed);
        }
        helper.assertBlockPresent(Blocks.LEAF_LITTER, natural);
        helper.assertBlockProperty(natural, SegmentableBlock.AMOUNT, 3);
        helper.assertBlockProperty(natural, LeafLitterBlock.FACING, Direction.EAST);
        helper.assertBlockPresent(CustomBlocks.FALLEN_LEAVES, placed);
        helper.assertBlockProperty(placed, SegmentableBlock.AMOUNT, 3);
        helper.succeed();
    }

    /** Shears and Silk Touch keep fallen leaves; anything else gives leaf litter, a pile's worth either way. */
    @GameTest(structure = TOWER)
    public void fallenLeavesDropThemselvesOnlyToShearsAndSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        ItemStack silkTouch = new ItemStack(Items.DIAMOND_PICKAXE);
        silkTouch.enchant(EnchantRegistryHelper.getRegistryEntry(level, Enchantments.SILK_TOUCH), 1);
        assertDrops(helper, Block.getDrops(fallenLeaves(3, false), level, pos, null, null, new ItemStack(Items.SHEARS)), CustomBlockItems.FALLEN_LEAVES, "shears");
        assertDrops(helper, Block.getDrops(fallenLeaves(3, false), level, pos, null, null, silkTouch), CustomBlockItems.FALLEN_LEAVES, "Silk Touch");
        assertDrops(helper, Block.getDrops(fallenLeaves(3, false), level, pos, null, null, ItemStack.EMPTY), Items.LEAF_LITTER, "a bare hand");
        assertDrops(helper, Block.getDrops(fallenLeaves(3, true), level, pos, null, null, ItemStack.EMPTY), Items.LEAF_LITTER, "a bare hand on a placed pile");
        helper.succeed();
    }

    /** Three segments of fallen leaves broken with this tool dropped three of this item and nothing else. */
    private static void assertDrops(GameTestHelper helper, List<ItemStack> drops, Item item, String tool) {
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 3,
                "three segments of fallen leaves broken with " + tool + " dropped " + drops);
    }

    /** Fallen leaves a player places are theirs: they never brown. */
    @GameTest(structure = TOWER)
    public void placedFallenLeavesArePersistent(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(CustomBlockItems.FALLEN_LEAVES),
                new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false));
        BlockState placed = CustomBlocks.FALLEN_LEAVES.getStateForPlacement(context);
        helper.assertTrue(placed != null && placed.getValue(FallenLeavesBlock.PERSISTENT), "placed fallen leaves aren't persistent: " + placed);
        helper.succeed();
    }

    /** Fallen leaves compost as leaf litter does. */
    @GameTest(structure = TOWER)
    public void fallenLeavesCompostLikeLeafLitter(GameTestHelper helper) {
        Compostable litter = Items.LEAF_LITTER.components().get(DataComponents.COMPOSTABLE);
        Compostable fallen = CustomBlockItems.FALLEN_LEAVES.components().get(DataComponents.COMPOSTABLE);
        helper.assertTrue(litter != null && litter.equals(fallen), "fallen leaves compost as " + fallen + ", leaf litter as " + litter);
        helper.succeed();
    }

    /** A pile of leaf litter is ground cover, not a cushion: landing on it hurts as much as landing on grass. */
    @GameTest(structure = TOWER, maxTicks = 100)
    public void aPileOfLitterDoesNotSoftenALanding(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 4, Blocks.GRASS_BLOCK.defaultBlockState());
        fill(helper, 1, 2, 1, 3, 2, 3, Blocks.LEAF_LITTER.defaultBlockState().setValue(SegmentableBlock.AMOUNT, 4));
        Mob onLitter = husk(helper, 2.5, 13.0, 2.5), onGrass = husk(helper, 8.5, 13.0, 2.5);
        helper.runAfterDelay(70, () -> {
            double hurtLitter = damageTaken(onLitter), hurtGrass = damageTaken(onGrass);
            VersusMod.MOD_LOGGER.info("Leaves: an 11-block fall hurt a husk {} onto grass, {} onto four segments of litter", hurtGrass, hurtLitter);
            helper.assertTrue(hurtGrass >= 6.0, "an 11-block fall onto grass did only " + hurtGrass);
            helper.assertTrue(hurtLitter == hurtGrass, "onto a pile of litter " + hurtLitter + ", onto grass " + hurtGrass);
            helper.succeed();
        });
    }
}
