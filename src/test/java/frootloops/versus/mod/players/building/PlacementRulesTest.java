package frootloops.versus.mod.players.building;

import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementRulesTest {

    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** The blocks around a placement, air where none is set. */
    private static final class World implements PlacementRules.View {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        World with(BlockPos pos, BlockState state) {
            blocks.put(pos, state);
            return this;
        }

        @Override
        public BlockState state(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public boolean sturdy(BlockPos pos, Direction face) {
            return state(pos).isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, face);
        }
    }

    private static BlockState resolve(BlockState vanilla, Direction clickedFace, World world) {
        return PlacementRules.resolve(vanilla, POS, clickedFace, null, null, null, world);
    }

    @Test
    void aLogTakesTheAxisOfTheRowItExtends() {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        World row = new World().with(POS.east(), log.setValue(BlockStateProperties.AXIS, Direction.Axis.X))
                .with(POS.west(), log.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        assertEquals(Direction.Axis.X, resolve(log.setValue(BlockStateProperties.AXIS, Direction.Axis.Y), Direction.UP, row).getValue(BlockStateProperties.AXIS));
        // Alone, the face clicked decides
        assertEquals(Direction.Axis.Z, resolve(log.setValue(BlockStateProperties.AXIS, Direction.Axis.Z), Direction.NORTH, new World()).getValue(BlockStateProperties.AXIS));
    }

    @Test
    void stairsFaceTheWayOfTheRowTheyExtend() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
        World row = new World().with(POS.east(), stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH))
                .with(POS.west(), stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        BlockState placed = resolve(stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), Direction.UP, row);
        assertEquals(Direction.NORTH, placed.getValue(BlockStateProperties.HORIZONTAL_FACING));
        assertEquals(Half.BOTTOM, placed.getValue(BlockStateProperties.HALF), "a click on a top face means the bottom half");
    }

    @Test
    void aSlabOnTheSideOfAFullBlockTakesTheHalfOfTheSlabsBesideIt() {
        BlockState slab = Blocks.STONE_SLAB.defaultBlockState();
        BlockState top = slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        World world = new World().with(POS.south(), Blocks.STONE.defaultBlockState()).with(POS.east(), top).with(POS.west(), top);
        BlockState bottom = slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        assertEquals(SlabType.TOP, resolve(bottom, Direction.NORTH, world).getValue(BlockStateProperties.SLAB_TYPE));
        // Clicked on a top face, or on the side of a slab, the height is meant
        assertSame(bottom, resolve(bottom, Direction.UP, world));
        world.with(POS.south(), bottom);
        assertSame(bottom, resolve(bottom, Direction.NORTH, world));
    }

    @Test
    void theLastPlacementSettlesAGuessedHalf() {
        BlockState slab = Blocks.STONE_SLAB.defaultBlockState();
        World world = new World().with(POS.south(), Blocks.STONE.defaultBlockState());
        BlockState bottom = slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        BlockState top = slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        assertEquals(SlabType.TOP, PlacementRules.resolve(bottom, POS, Direction.NORTH, null, top, null, world).getValue(BlockStateProperties.SLAB_TYPE));
        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP);
        assertEquals(Half.BOTTOM, PlacementRules.resolve(trapdoor, POS, Direction.NORTH, null, trapdoor.setValue(BlockStateProperties.HALF, Half.BOTTOM), null, world)
                .getValue(BlockStateProperties.HALF));
    }

    @Test
    void aStrokeKeepsItsFirstBlocksTurn() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
        BlockState first = stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).setValue(BlockStateProperties.HALF, Half.TOP);
        BlockState placed = PlacementRules.resolve(stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), POS, Direction.UP, first, null, null, new World());
        assertEquals(Direction.SOUTH, placed.getValue(BlockStateProperties.HORIZONTAL_FACING));
        assertEquals(Half.TOP, placed.getValue(BlockStateProperties.HALF));
        // An observer turns only with a stroke
        BlockState observer = Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
        assertSame(observer, resolve(observer, Direction.UP, new World().with(POS.east(), observer.setValue(BlockStateProperties.FACING, Direction.NORTH))));
        assertEquals(Direction.NORTH, PlacementRules.resolve(observer, POS, Direction.UP, observer.setValue(BlockStateProperties.FACING, Direction.NORTH), null, null, new World())
                .getValue(BlockStateProperties.FACING));
    }

    @Test
    void whatABlockHangsOnIsNeverTurned() {
        BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        assertSame(torch, PlacementRules.resolve(torch, POS, Direction.NORTH, torch.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), null, null, new World()));
    }

    @Test
    void furnacesLineUpWithTheirOwnKind() {
        BlockState furnace = Blocks.FURNACE.defaultBlockState();
        BlockState south = furnace.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        World row = new World().with(POS.east(), south).with(POS.west(), south);
        assertEquals(Direction.SOUTH, resolve(furnace.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), Direction.UP, row)
                .getValue(BlockStateProperties.HORIZONTAL_FACING));
        // One in front of it doesn't count: it isn't in a row with it
        World inFront = new World().with(POS.south(), south);
        BlockState east = furnace.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        assertSame(east, resolve(east, Direction.UP, inFront));
    }

    @Test
    void aFenceGateFacesAcrossItsFence() {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        World line = new World().with(POS.east(), fence).with(POS.west(), fence);
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        Vec3 north = new Vec3(0.5, 65.6, -2.0);
        assertEquals(Direction.SOUTH, PlacementRules.resolve(gate, POS, Direction.UP, null, null, north, line).getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    @Test
    void signsTurnLikeTheOnesBesideThemWhenClose() {
        BlockState sign = Blocks.OAK_SIGN.defaultBlockState();
        World world = new World().with(POS.east(), sign.setValue(BlockStateProperties.ROTATION_16, 4));
        assertEquals(4, resolve(sign.setValue(BlockStateProperties.ROTATION_16, 5), Direction.UP, world).getValue(BlockStateProperties.ROTATION_16));
        assertEquals(8, resolve(sign.setValue(BlockStateProperties.ROTATION_16, 8), Direction.UP, world).getValue(BlockStateProperties.ROTATION_16));
    }

    @Test
    void theServerTakesOnlyATurnHalfOrRotation() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
        assertTrue(PlacementRules.acceptable(stairs, stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).setValue(BlockStateProperties.HALF, Half.TOP)));
        assertFalse(PlacementRules.acceptable(stairs, stairs.setValue(BlockStateProperties.WATERLOGGED, true)));
        assertFalse(PlacementRules.acceptable(stairs, Blocks.STONE_STAIRS.defaultBlockState()));
        BlockState slab = Blocks.STONE_SLAB.defaultBlockState();
        assertTrue(PlacementRules.acceptable(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)));
        assertFalse(PlacementRules.acceptable(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE)));
    }

    @Test
    void aJustPlacedBlockBreaksFasterButNeverAtOnce() {
        assertEquals(0.2F, RecentPlacements.faster(0.01F), 1e-6);
        assertEquals(0.4F, RecentPlacements.faster(0.1F), 1e-6);
        assertEquals(0.5F, RecentPlacements.faster(0.3F), 1e-6);
        assertEquals(0.7F, RecentPlacements.faster(0.7F), 1e-6);
        assertEquals(1.0F, RecentPlacements.faster(1.0F), 1e-6);
        assertEquals(0.0F, RecentPlacements.faster(0.0F), 1e-6);
    }

    /** The placement mixins apply on 26.4: their handlers are in the classes. */
    @Test
    void thePlacementMixinsApply() {
        assertTrue(hasHandler(BlockItem.class, "playersVersus$turnToMatch"), "block items: the state placed");
        assertTrue(hasHandler(BlockItem.class, "playersVersus$rememberPlaced"), "block items: the blocks placed");
        assertTrue(hasHandler(StandingAndWallBlockItem.class, "playersVersus$turnToMatch"), "signs, banners, heads and torches");
        assertTrue(hasHandler(BlockBehaviour.class, "playersVersus$takeBackPlaced"), "mining a block just placed");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(method -> method.endsWith(name));
    }
}
