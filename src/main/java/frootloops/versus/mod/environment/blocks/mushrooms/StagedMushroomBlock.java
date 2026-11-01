package frootloops.versus.mod.environment.blocks.mushrooms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Small mushrooms grown together, two or three (MushroomGrowth): brown or red ones, whose single mushroom is vanilla's
 * block, or pale shrooms, one to three. Brown and red ones give off more light the more they are (pale shrooms give off
 * none), stand where a mushroom stands, and grow on random ticks (StagedMushrooms). Pale shrooms blight whatever stays in
 * them, very slowly (BlightExposure).
 */
public class StagedMushroomBlock extends VegetationBlock {
    public static final IntegerProperty MUSHROOMS = IntegerProperty.create("mushrooms", 1, 3);
    private static final VoxelShape[] SHAPES = {
            Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0)
    };

    public final MushroomGrowth.Kind kind;

    public StagedMushroomBlock(MushroomGrowth.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        // Brown and red ones start at two: one alone is vanilla's mushroom
        this.registerDefaultState(this.stateDefinition.any().setValue(MUSHROOMS, kind == MushroomGrowth.Kind.PALE ? 1 : 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MUSHROOMS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(MUSHROOMS) - 1];
    }

    /** Where a mushroom stands: on a face that holds its middle up (as vanilla's mushrooms). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        StagedMushrooms.randomTick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (this.kind == MushroomGrowth.Kind.PALE && level instanceof ServerLevel serverLevel) BlightExposure.inside(serverLevel, entity);
    }

    /** Brown and red ones are vanilla's mushroom, more of them: picking the block picks that mushroom. */
    @Override
    public Item asItem() {
        return switch (this.kind) {
            case BROWN -> Items.BROWN_MUSHROOM;
            case RED -> Items.RED_MUSHROOM;
            case PALE -> super.asItem();
        };
    }
}
