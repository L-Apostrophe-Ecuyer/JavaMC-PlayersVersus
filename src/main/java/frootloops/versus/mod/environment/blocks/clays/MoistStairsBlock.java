package frootloops.versus.mod.environment.blocks.clays;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class MoistStairsBlock extends StairsBlock implements MoistureConvertableBlock {

    private final Block dryVersion;
    private final Block cookedVersion;
    public Block wetterVersion;

    public MoistStairsBlock(BlockState state, Block dryVersion) {
        this(state, dryVersion, dryVersion);
    }

    public MoistStairsBlock(BlockState state, Block dryVersion, Block cookedVersion) {
        this(state, dryVersion, cookedVersion, null);
    }

    public MoistStairsBlock(BlockState state, Block dryVersion, Block cookedVersion, Block wetterVersion) {
        super(state, AbstractBlock.Settings.copy(state.getBlock()));
        this.dryVersion = dryVersion;
        this.cookedVersion = cookedVersion;
        this.wetterVersion = wetterVersion;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        MoistureConvertableBlock.scheduledTick(state, world, pos, dryVersion, cookedVersion, wetterVersion);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        MoistureConvertableBlock.scheduledTick(state, world, pos, dryVersion, cookedVersion, wetterVersion);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        MoistureConvertableBlock.onLandedUpon(world, state, pos, entity, fallDistance, dryVersion);
        super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5F);
    }
}
