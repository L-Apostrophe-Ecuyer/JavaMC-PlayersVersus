package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.leaves.FallenLeavesBlock;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Leaves as husks (which don't burn) find them on the game test server: pushing through, climbing, sinking, falling onto
 * and through them, and the litter they shed. Mobs move on the server, so this is the movement a player's client
 * simulates for itself through the same code (LeafMovement).
 */
public class LeavesGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room";   // 14 x 8 x 8
    private static final String TOWER = "players-versus-gametest:leaves_tower"; // 12 x 24 x 6
    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

    /** Persistent, so they don't decay while the test runs. */
    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A tree's own leaves, this far from its logs. */
    private static BlockState natural(Block block, int distance) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, distance);
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

    /**
     * Leaves underwater hold back a swimmer as leaves hold back a walker: a husk wading along a flooded tunnel goes about
     * half as far where the tunnel is full of waterlogged leaves.
     */
    @GameTest(structure = ROOM, maxTicks = 60)
    public void leavesUnderwaterHoldBackASwimmer(GameTestHelper helper) {
        fill(helper, 0, 0, 0, 13, 3, 7, Blocks.STONE.defaultBlockState());
        fill(helper, 1, 1, 1, 12, 2, 2, Blocks.WATER.defaultBlockState());
        fill(helper, 1, 1, 5, 12, 2, 6, leaves(Blocks.OAK_LEAVES).setValue(LeavesBlock.WATERLOGGED, true));
        Mob open = husk(helper, 1.5, 1.0, 1.5), leafy = husk(helper, 1.5, 1.0, 5.5);
        double startX = helper.relativeVec(open.position()).x;
        helper.onEachTick(() -> {
            for (Mob husk : new Mob[]{open, leafy}) {
                Vec3 target = helper.absoluteVec(new Vec3(12.5, 1.0, helper.relativeVec(husk.position()).z));
                husk.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.5);
            }
        });
        helper.runAfterDelay(40, () -> {
            double swamOpen = helper.relativeVec(open.position()).x - startX;
            double swamLeafy = helper.relativeVec(leafy.position()).x - startX;
            VersusMod.MOD_LOGGER.info("Leaves: in 40 ticks underwater a husk went {} in open water, {} through leaves", swamOpen, swamLeafy);
            helper.assertTrue(swamOpen > 1.0, "the husk in open water went only " + swamOpen);
            helper.assertTrue(swamLeafy < 0.85 * swamOpen, "through the leaves " + swamLeafy + ", in open water " + swamOpen);
            helper.assertTrue(swamLeafy > 0.2 * swamOpen, "the leaves nearly stopped it: " + swamLeafy + " against " + swamOpen);
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

    /** A drop of up to four blocks lands on top of leaves; a longer one breaks through them to the ground. */
    @GameTest(structure = TOWER, maxTicks = 80)
    public void aShortDropLandsOnTopALongOneBreaksThrough(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 0, 1, 1, 2, 1, 3, leaves(Blocks.OAK_LEAVES));
        fill(helper, 4, 1, 1, 6, 1, 3, leaves(Blocks.OAK_LEAVES));
        fill(helper, 8, 1, 1, 10, 1, 3, leaves(Blocks.OAK_LEAVES));
        Mob dropped = husk(helper, 1.5, 3.5, 2.5), fourBlocks = husk(helper, 5.5, 6.0, 2.5), thrown = husk(helper, 9.5, 8.0, 2.5);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(relativeY(helper, dropped) > 1.99, "a 1.5-block drop sank to " + relativeY(helper, dropped));
            helper.assertTrue(dropped.onGround(), "a 1.5-block drop isn't standing on the leaf");
            helper.assertTrue(relativeY(helper, fourBlocks) > 1.99, "a 4-block drop sank to " + relativeY(helper, fourBlocks));
            helper.assertTrue(relativeY(helper, thrown) < 1.5, "a 6-block drop stayed at " + relativeY(helper, thrown));
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

    /**
     * The server checks every move a player's client reports: it snaps the player back if the move ran into anything
     * new (26.4's pre-move collisions, asked as from where the player was), and looks for the block holding a player up
     * who says they're on the ground ("standing on air" when there's none). Both must see leaves as the player does.
     */
    @GameTest(structure = ROOM)
    public void theServersMoveCheckSeesLeavesAsThePlayerDoes(GameTestHelper helper) {
        floor(helper, 14, 8);
        helper.setBlock(3, 1, 2, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(3, 2, 2, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(6, 1, 2, leaves(Blocks.OAK_LEAVES));
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Walking into a hedge: the server snaps a player back if the move ran into anything new
        Vec3 beside = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.setPos(beside.x, beside.y, beside.z);
        AABB before = player.getBoundingBox();
        AABB into = before.move(0.6, 0.0, 0.0).deflate(1.0E-5);
        int hit = 0;
        for (VoxelShape shape : level.getPreMoveCollisions(player, into, before.getBottomCenter())) {
            if (Shapes.joinIsNotEmpty(shape, Shapes.create(into), BooleanOp.AND)) hit++;
        }
        CollisionContext moveCheck = CollisionContext.withPosition(player, before.minY);
        helper.assertTrue(hit == 0, "the server's move check ran into " + hit + " leaf shapes (its context: placement "
                + moveCheck.isPlacement() + ")");

        // Standing on a leaf: the server looks for the block holding the player up
        Vec3 onTop = helper.absoluteVec(new Vec3(6.5, 2.0, 2.5));
        player.setPos(onTop.x, onTop.y, onTop.z);
        AABB feet = player.getBoundingBox();
        boolean held = level.findSupportingBlock(player, new AABB(feet.minX, feet.minY - 1.0E-6, feet.minZ, feet.maxX, feet.minY, feet.maxZ)).isPresent();
        helper.assertTrue(held, "the server found nothing under a player standing on a leaf");
        helper.succeed();
    }

    /**
     * The server holds a player up on a leaf as their client does, and as it holds a mob; placing a block isn't held
     * back by leaves, which anything that passes through them could be standing in.
     */
    @GameTest(structure = ROOM)
    public void theServerHoldsPlayersUpAsTheirClientDoes(GameTestHelper helper) {
        floor(helper, 14, 8);
        helper.setBlock(2, 1, 2, leaves(Blocks.OAK_LEAVES));
        BlockPos leaf = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 onTop = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(onTop.x, onTop.y, onTop.z);
        Mob husk = husk(helper, 2.5, 2.0, 2.5);
        helper.assertTrue(LeafMovement.isSolid(leaf, CollisionContext.of(player)), "the server didn't hold a player up on a leaf");
        helper.assertTrue(LeafMovement.isSolid(leaf, CollisionContext.of(husk)), "a husk above a leaf isn't held up");
        helper.assertFalse(LeafMovement.isSolid(leaf, CollisionContext.placementContext(player)), "placing a block was held back by a leaf");
        helper.succeed();
    }

    /**
     * Shed leaves fall through the leaves below to the ground and pile up to four: fallen leaves from oak, leaf litter
     * from poplars, petals from cherry, nothing from spruce. A falling leaf joins any leaf pile it lands on, never onto
     * water or into a plant.
     */
    @GameTest(structure = TOWER)
    public void shedLeavesFallThroughTheCanopyAndPileUp(GameTestHelper helper) {
        floor(helper, 12, 6);
        fill(helper, 1, 1, 1, 10, 1, 3, Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(1, 6, 1, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(1, 7, 1, leaves(Blocks.OAK_LEAVES));
        for (int i = 0; i < 4; i++) helper.assertTrue(shedFrom(helper, 1, 7, 1, Blocks.OAK_LEAVES), "segment " + (i + 1) + " didn't land");
        helper.assertFalse(shedFrom(helper, 1, 7, 1, Blocks.OAK_LEAVES), "a fifth segment landed on a full pile");
        helper.assertBlockPresent(CustomBlocks.FALLEN_LEAVES, new BlockPos(1, 2, 1));
        helper.assertBlockProperty(new BlockPos(1, 2, 1), SegmentableBlock.AMOUNT, 4);

        helper.assertTrue(shedFrom(helper, 3, 6, 1, Blocks.RED_POPLAR_LEAVES), "poplar leaves shed nothing");
        helper.assertBlockPresent(Blocks.LEAF_LITTER, new BlockPos(3, 2, 1));
        helper.assertTrue(shedFrom(helper, 5, 6, 1, Blocks.CHERRY_LEAVES), "cherry leaves shed nothing");
        helper.assertBlockPresent(Blocks.PINK_PETALS, new BlockPos(5, 2, 1));
        helper.assertFalse(shedFrom(helper, 6, 6, 1, Blocks.SPRUCE_LEAVES), "spruce leaves shed something");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 1));

        helper.setBlock(8, 2, 1, Blocks.LEAF_LITTER.defaultBlockState().setValue(SegmentableBlock.AMOUNT, 2));
        helper.assertTrue(shedFrom(helper, 8, 6, 1, Blocks.OAK_LEAVES), "an oak leaf didn't join the leaf litter");
        helper.assertBlockPresent(Blocks.LEAF_LITTER, new BlockPos(8, 2, 1));
        helper.assertBlockProperty(new BlockPos(8, 2, 1), SegmentableBlock.AMOUNT, 3);
        helper.setBlock(9, 2, 1, fallenLeaves(1, true));
        helper.assertTrue(shedFrom(helper, 9, 6, 1, Blocks.OAK_LEAVES), "an oak leaf didn't join a placed pile of fallen leaves");
        helper.assertBlockProperty(new BlockPos(9, 2, 1), SegmentableBlock.AMOUNT, 2);
        helper.assertBlockProperty(new BlockPos(9, 2, 1), FallenLeavesBlock.PERSISTENT, true);
        helper.setBlock(10, 2, 1, Blocks.LEAF_LITTER.defaultBlockState().setValue(SegmentableBlock.AMOUNT, 4));
        helper.assertFalse(shedFrom(helper, 10, 6, 1, Blocks.OAK_LEAVES), "an oak leaf landed on a full pile of leaf litter");

        helper.setBlock(7, 1, 3, Blocks.WATER.defaultBlockState());
        helper.assertFalse(shedFrom(helper, 7, 6, 3, Blocks.OAK_LEAVES), "a leaf landed on water");
        helper.setBlock(9, 2, 3, Blocks.SHORT_GRASS.defaultBlockState());
        helper.assertFalse(shedFrom(helper, 9, 6, 3, Blocks.OAK_LEAVES), "a leaf replaced short grass");
        helper.succeed();
    }

    /** One segment shed from natural leaves of this kind at (x, y, z). */
    private static boolean shedFrom(GameTestHelper helper, int x, int y, int z, Block kind) {
        ServerLevel level = helper.getLevel();
        return LeafLitter.shed(level, helper.absolutePos(new BlockPos(x, y, z)), leaves(kind), level.getRandom());
    }

    /** Dying leaves leave fallen leaves under them by chance: a whole layer of them leaves some. */
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
                if (ground.is(CustomBlocks.FALLEN_LEAVES)) segments += ground.getValue(SegmentableBlock.AMOUNT);
            }
        }
        VersusMod.MOD_LOGGER.info("Leaves: 40 decaying leaves left {} segments of fallen leaves", segments);
        helper.assertTrue(segments > 0, "40 decaying leaves left no fallen leaves");
        helper.assertTrue(segments < 40, "every decaying leaf left fallen leaves");
        helper.succeed();
    }

    /**
     * Cutting a tree's trunk drops its natural crown at once, not leaf by leaf over minutes as vanilla; a placed leaf
     * stays, and a waterlogged one leaves its water.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aFelledTreeDropsItsCrownAtOnce(GameTestHelper helper) {
        floor(helper, 14, 8);
        fill(helper, 3, 1, 3, 3, 4, 3, Blocks.OAK_LOG.defaultBlockState());
        for (int x = 2; x <= 4; x++) {
            for (int y = 3; y <= 5; y++) {
                for (int z = 2; z <= 4; z++) {
                    if (x == 3 && z == 3 && y <= 4) continue;
                    helper.setBlock(x, y, z, natural(Blocks.OAK_LEAVES, Math.abs(x - 3) + Math.abs(z - 3) + (y == 5 ? 1 : 0)));
                }
            }
        }
        helper.setBlock(4, 4, 4, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(2, 3, 3, helper.getBlockState(new BlockPos(2, 3, 3)).setValue(LeavesBlock.WATERLOGGED, true));
        fill(helper, 3, 1, 3, 3, 4, 3, Blocks.AIR.defaultBlockState());
        helper.succeedWhen(() -> {
            for (BlockPos pos : BlockPos.betweenClosed(2, 3, 2, 4, 5, 4)) {
                BlockState state = helper.getBlockState(pos);
                helper.assertFalse(state.is(Blocks.OAK_LEAVES) && !state.getValue(LeavesBlock.PERSISTENT), "a natural leaf stayed at " + pos.toShortString());
            }
            helper.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(4, 4, 4));
            helper.assertBlockPresent(Blocks.WATER, new BlockPos(2, 3, 3));
            VersusMod.MOD_LOGGER.info("Leaves: a felled oak's crown was gone within {} ticks", helper.getTick());
        });
    }

    /**
     * A leaf lives only by its own tree's logs: an oak leaf by a birch log falls, one by its own log stripped stays, and
     * so does one by a jungle log, which is what a jungle bush grows oak leaves on.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void leavesLiveOnlyByTheirOwnTreesLogs(GameTestHelper helper) {
        floor(helper, 14, 8);
        List<Block> logs = List.of(Blocks.BIRCH_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.JUNGLE_LOG);
        for (int i = 0; i < logs.size(); i++) {
            helper.setBlock(2 + 4 * i, 1, 2, logs.get(i).defaultBlockState());
            helper.setBlock(2 + 4 * i, 2, 2, natural(Blocks.OAK_LEAVES, 1));
            helper.getLevel().scheduleTick(helper.absolutePos(new BlockPos(2 + 4 * i, 2, 2)), Blocks.OAK_LEAVES, 1);
        }
        helper.runAfterDelay(5, () -> {
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, new BlockPos(2, 2, 2));
            helper.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(6, 2, 2));
            helper.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(10, 2, 2));
            helper.succeed();
        });
    }

    /** A placed leaf holds no tree's reach: a natural leaf joined to its log only through one is cut off. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aPlacedLeafHoldsUpNoNaturalOne(GameTestHelper helper) {
        floor(helper, 14, 8);
        helper.setBlock(2, 1, 2, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(3, 1, 2, leaves(Blocks.OAK_LEAVES));
        helper.setBlock(4, 1, 2, natural(Blocks.OAK_LEAVES, 2));
        helper.getLevel().scheduleTick(helper.absolutePos(new BlockPos(4, 1, 2)), Blocks.OAK_LEAVES, 1);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(3, 1, 2));
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, new BlockPos(4, 1, 2));
            helper.succeed();
        });
    }

    /** Every tree keeps all its leaves through an update of each: the mod's rules never doom a natural tree's own leaves. */
    @GameTest(structure = GROVE, maxTicks = 40)
    public void everyTreeKeepsItsLeaves(GameTestHelper helper) {
        treesKeepTheirLeaves(helper, List.of(TreeFeatures.OAK, TreeFeatures.FANCY_OAK, TreeFeatures.BIRCH, TreeFeatures.SPRUCE, TreeFeatures.PINE,
                TreeFeatures.ACACIA, TreeFeatures.DARK_OAK, TreeFeatures.SWAMP_OAK, TreeFeatures.JUNGLE_BUSH));
    }

    /** The rest of the trees, as everyTreeKeepsItsLeaves. */
    @GameTest(structure = GROVE, maxTicks = 40)
    public void everyOtherTreeKeepsItsLeaves(GameTestHelper helper) {
        treesKeepTheirLeaves(helper, List.of(TreeFeatures.JUNGLE_TREE, TreeFeatures.MEGA_SPRUCE, TreeFeatures.AZALEA_TREE, TreeFeatures.MANGROVE,
                TreeFeatures.CHERRY, TreeFeatures.PALE_OAK, TreeFeatures.RED_POPLAR, TreeFeatures.ORANGE_POPLAR, TreeFeatures.YELLOW_POPLAR));
    }

    /**
     * Grows these nine trees in a 3 x 3 grid 16 apart, gives every natural leaf of theirs that vanilla would keep a tick,
     * and checks a few ticks later that each tree still has them all.
     */
    private static void treesKeepTheirLeaves(GameTestHelper helper, List<ResourceKey<Feature>> trees) {
        ServerLevel level = helper.getLevel();
        List<Map<BlockPos, Integer>> before = new ArrayList<>();
        for (int t = 0; t < trees.size(); t++) {
            Feature tree = level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(trees.get(t)).value();
            BlockPos origin = helper.absolutePos(new BlockPos(8 + 16 * (t % 3), 1, 8 + 16 * (t / 3)));
            helper.assertTrue(tree.place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), origin), trees.get(t).identifier() + " didn't grow");
        }
        for (int t = 0; t < trees.size(); t++) {
            before.add(livingLeaves(helper, t));
            helper.assertTrue(!before.get(t).isEmpty(), trees.get(t).identifier() + " grew no leaves");
            for (BlockPos pos : before.get(t).keySet()) level.scheduleTick(helper.absolutePos(pos), helper.getBlockState(pos).getBlock(), 1);
        }
        helper.runAfterDelay(5, () -> {
            List<String> lost = new ArrayList<>(), kept = new ArrayList<>();
            for (int t = 0; t < trees.size(); t++) {
                Map<BlockPos, Integer> after = livingLeaves(helper, t);
                String tree = trees.get(t).identifier().getPath();
                List<String> gone = before.get(t).entrySet().stream().filter(leaf -> !after.containsKey(leaf.getKey()))
                        .map(leaf -> leaf.getKey().toShortString() + " at " + leaf.getValue() + ", now " + helper.getBlockState(leaf.getKey())).toList();
                if (gone.isEmpty()) kept.add(tree + " " + after.size());
                else lost.add(tree + " lost " + gone.size() + " of its " + before.get(t).size() + " leaves (" + String.join("; ", gone.subList(0, Math.min(3, gone.size()))) + ")");
            }
            helper.assertTrue(lost.isEmpty(), String.join(", ", lost));
            VersusMod.MOD_LOGGER.info("Leaves: every tree kept its leaves ({})", String.join(", ", kept));
            helper.succeed();
        });
    }

    /** The natural leaves in this tree's 16 x 16 cell that vanilla would keep (closer than 7 to a log), and how close. */
    private static Map<BlockPos, Integer> livingLeaves(GameTestHelper helper, int tree) {
        int x0 = 16 * (tree % 3), z0 = 16 * (tree / 3);
        Map<BlockPos, Integer> living = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(x0, 1, z0, x0 + 15, 39, z0 + 15)) {
            BlockState state = helper.getBlockState(pos);
            if (!(state.getBlock() instanceof LeavesBlock) || state.getValue(LeavesBlock.PERSISTENT) || state.getValue(LeavesBlock.DISTANCE) >= 7) continue;
            living.put(pos.immutable(), state.getValue(LeavesBlock.DISTANCE));
        }
        return living;
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

    /** The segments of fallen leaves under natural oak leaves at (x, 6, 2) after a hundred ticks of this entity inside them. */
    private static int litterAfterAWhileIn(GameTestHelper helper, int x, Entity entity) {
        BlockState natural = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
        BlockPos leaves = helper.absolutePos(new BlockPos(x, 6, 2));
        for (int i = 0; i < 100; i++) LeafLitter.disturb(helper.getLevel(), leaves, natural, entity);
        BlockState ground = helper.getBlockState(new BlockPos(x, 2, 2));
        return ground.is(CustomBlocks.FALLEN_LEAVES) ? ground.getValue(SegmentableBlock.AMOUNT) : 0;
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
