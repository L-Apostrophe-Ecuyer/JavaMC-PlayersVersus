package frootloops.versus.mod.environment.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class BrownMudStairsBlock extends StairsBlock implements PackedMudConvertableBlock {

    private final Block mudstoneVersion;

    public BrownMudStairsBlock(BlockState baseBlockState, Settings settings, Block mudstoneVersion) {
        super(baseBlockState, settings);
        this.mudstoneVersion = mudstoneVersion;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        PackedMudConvertableBlock.scheduledTick(state, world, pos, mudstoneVersion);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        PackedMudConvertableBlock.scheduledTick(state, world, pos, mudstoneVersion);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        PackedMudConvertableBlock.onLandedUpon(world, state, pos, entity, fallDistance, mudstoneVersion);
        super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5F);
    }
}
