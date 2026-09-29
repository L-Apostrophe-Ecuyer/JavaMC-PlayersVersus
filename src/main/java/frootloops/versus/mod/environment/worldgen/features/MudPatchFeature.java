package frootloops.versus.mod.environment.worldgen.features;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class MudPatchFeature extends Feature<MudPatchFeatureConfig> {
    private static final ImmutableList<Block> CAN_REPLACE_BLOCKS = ImmutableList.of(
            Blocks.STONE, Blocks.DEEPSLATE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.CLAY, Blocks.MUD, Blocks.PACKED_MUD, CustomBlocks.GRAY_CLAY, CustomBlocks.BROWN_CLAY
    );
    private static final ImmutableList<Block> BLOCKS_ALWAYS_GRAY = ImmutableList.of(
            Blocks.STONE, Blocks.DEEPSLATE
    );

    private static final Direction[] DIRECTIONS = new Direction[]{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST};
    private static final BlockState BROWN_MUD = CustomBlocks.BROWN_MUD.defaultBlockState(), BROWN_MUD_DRY = CustomBlocks.BROWN_MUD.getDryVersion().defaultBlockState(), BROWN_MUD_COOKED = CustomBlocks.BROWN_MUD.getCookedVersion().defaultBlockState();
    private static final BlockState GRAY_MUD = CustomBlocks.GRAY_MUD.defaultBlockState(), GRAY_MUD_DRY = CustomBlocks.GRAY_MUD.getDryVersion().defaultBlockState(), GRAY_MUD_COOKED = CustomBlocks.GRAY_MUD.getCookedVersion().defaultBlockState();

    public MudPatchFeature(Codec<MudPatchFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MudPatchFeatureConfig> context) {
        boolean wasAbleToGenerate = false;
        RandomSource random = context.random();
        WorldGenLevel world = context.level();
        MudPatchFeatureConfig config = context.config();
        BlockPos centerPos = context.origin();

        BlockState wetBlock = config.isBrownMud() ? BROWN_MUD : GRAY_MUD;
        BlockState dryBlock = config.isBrownMud() ? BROWN_MUD_DRY : GRAY_MUD_DRY;
        BlockState cookedBlock = config.isBrownMud() ? BROWN_MUD_COOKED : GRAY_MUD_COOKED;

        int size = config.size().sample(random);
        double sizeSquared = (double) (size * size);
        double centerX = centerPos.getX() + 0.5;
        double centerZ = centerPos.getZ() + 0.5;
        while(world.getBlockState(centerPos).isAir()) centerPos = centerPos.below();
        while(!world.getBlockState(centerPos.above()).isAir()) centerPos = centerPos.above();

        for (BlockPos blockPos : BlockPos.withinManhattan(centerPos, size, 0, size)) {
            double distX = blockPos.getX() - centerX;
            double distZ = blockPos.getZ() - centerZ;
            double squaredDist = distX * distX + distZ * distZ;
            if(squaredDist > sizeSquared) continue;

            // Get the correct Y level to place at. If not valid, skip it;
            BlockState blockState = world.getBlockState(blockPos);
            if(blockState.canBeReplaced()) blockPos = blockPos.below();
            else if(!world.getBlockState(blockPos.above()).canBeReplaced()) blockPos = blockPos.above();
            if(!world.getBlockState(blockPos.above()).canBeReplaced() || !CAN_REPLACE_BLOCKS.contains(blockState.getBlock())) continue;

            // Random chance to stop if near edge:
            boolean isNearCenter = squaredDist/sizeSquared < 0.5;
            if(!isNearCenter && random.nextDouble() > squaredDist/sizeSquared) continue;

            // Get the correct block type for position:
            BlockState toPlace;
            if(config.isBrownMud() && BLOCKS_ALWAYS_GRAY.contains(blockState.getBlock())) toPlace = getBlockToPlace(world, blockPos, random, isNearCenter, GRAY_MUD, GRAY_MUD_DRY, GRAY_MUD_COOKED);
            else toPlace = getBlockToPlace(world, blockPos, random, isNearCenter, wetBlock, dryBlock, cookedBlock);
            if(toPlace == wetBlock && !isNearCenter && random.nextDouble() > squaredDist/sizeSquared) toPlace = dryBlock;
            if(toPlace == null || blockState.is(toPlace.getBlock())) continue;

            // Place block:
            world.setBlock(blockPos, toPlace, Block.UPDATE_CLIENTS);
            if(toPlace.is(Blocks.WATER)) world.scheduleTick(blockPos, toPlace.getFluidState().getType(), 0);
            wasAbleToGenerate = true;

            // Next, if were in the middle of the pool:
            if(isNearCenter && (toPlace == wetBlock || toPlace.is(Blocks.WATER))) {

                // Random chance of placing sugar cane, if sunlit:
                if(toPlace == BROWN_MUD && world.getBlockState(blockPos.above()).isAir() && world.getBrightness(LightLayer.SKY, blockPos.above()) > 14 && random.nextInt(4) == 0) {
                    world.setBlock(blockPos.above(), Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }

                // Try placing a block below as well:
                blockPos = blockPos.below();
                blockState = world.getBlockState(blockPos);
                if(!world.getBlockState(blockPos.above()).canBeReplaced() || !CAN_REPLACE_BLOCKS.contains(blockState.getBlock())) continue;
                toPlace = config.isBrownMud() && BLOCKS_ALWAYS_GRAY.contains(blockState.getBlock()) ? GRAY_MUD : wetBlock;

                // Place block below:
                world.setBlock(blockPos, toPlace, Block.UPDATE_CLIENTS);
                if(toPlace.is(Blocks.WATER)) world.scheduleTick(blockPos, toPlace.getFluidState().getType(), 0);
            }
        }
        return wasAbleToGenerate;
    }

    private static BlockState getBlockToPlace(LevelAccessor world, BlockPos pos, RandomSource random, boolean isNearCenter, BlockState wetBlock, BlockState dryBlock, BlockState cookedBlock) {
        BlockState neighborState = world.getBlockState(pos.above());
        if(!neighborState.getFluidState().isEmpty()) {
            if(neighborState.getFluidState().is(FluidTags.LAVA)) return cookedBlock;
            else if(neighborState.getFluidState().is(FluidTags.WATER)) return isNearCenter && !world.getBlockState(pos.below()).isAir() ? Blocks.WATER.defaultBlockState() : wetBlock;
        }
        else if(neighborState.is(wetBlock.getBlock()) && neighborState.is(Blocks.DIRT)) return wetBlock;

        BlockState stateToReturn = wetBlock;
        int numAirBlocks = 0;
        for (Direction direction : DIRECTIONS) {
            neighborState = world.getBlockState(pos.relative(direction));
            if(!neighborState.getFluidState().isEmpty()) {
                if(neighborState.getFluidState().is(FluidTags.LAVA)) return cookedBlock;
                else if(neighborState.getFluidState().is(FluidTags.WATER)) return isNearCenter && random.nextBoolean() && !world.getBlockState(pos.below()).isAir() ? Blocks.WATER.defaultBlockState() : wetBlock;
            }
            else if (neighborState.canBeReplaced()) {
                stateToReturn = dryBlock;
                numAirBlocks++;
            }
        }
        if(isNearCenter && stateToReturn == wetBlock && random.nextFloat() < 0.1f && !world.getBlockState(pos.below()).isAir()) return Blocks.WATER.defaultBlockState();
        else return numAirBlocks == 4 ? null : stateToReturn;
    }
}
