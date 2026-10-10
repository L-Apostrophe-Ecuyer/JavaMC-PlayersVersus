package frootloops.versus.mod.environment.blocks.clays;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MoistWallBlock extends WallBlock implements MoistureConvertableBlock {

    private final Block dryVersion;
    private final Block cookedVersion;
    public Block wetterVersion;

    public MoistWallBlock(Properties settings, Block dryVersion) {
        this(settings, dryVersion, dryVersion);
    }

    public MoistWallBlock(Properties settings, Block dryVersion, Block cookedVersion) {
        this(settings, dryVersion, cookedVersion, null);
    }

    public MoistWallBlock(Properties settings, Block dryVersion, Block cookedVersion, Block wetterVersion) {
        super(settings);
        this.dryVersion = dryVersion;
        this.cookedVersion = cookedVersion;
        this.wetterVersion = wetterVersion;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        MoistureConvertableBlock.scheduledTick(state, world, pos, dryVersion, cookedVersion, wetterVersion);
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        MoistureConvertableBlock.scheduledTick(state, world, pos, dryVersion, cookedVersion, wetterVersion);
    }

    @Override
    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        MoistureConvertableBlock.onLandedUpon(world, state, pos, entity, fallDistance, dryVersion);
        super.fallOn(world, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public Block getDryVersion() {
        return this.dryVersion;
    }

    @Override
    public Block getCookedVersion() {
        return this.cookedVersion;
    }

    @Override
    public boolean hasWetVersion() {
        return wetterVersion != null;
    }
}
