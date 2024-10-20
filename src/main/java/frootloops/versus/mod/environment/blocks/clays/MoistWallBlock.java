package frootloops.versus.mod.environment.blocks.clays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class MoistWallBlock extends WallBlock implements MoistureConvertableBlock {

    private final Block dryVersion;
    private final Block cookedVersion;
    public Block wetterVersion;

    public MoistWallBlock(Settings settings, Block dryVersion) {
        this(settings, dryVersion, dryVersion);
    }

    public MoistWallBlock(Settings settings, Block dryVersion, Block cookedVersion) {
        this(settings, dryVersion, cookedVersion, null);
    }

    public MoistWallBlock(Settings settings, Block dryVersion, Block cookedVersion, Block wetterVersion) {
        super(settings);
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
