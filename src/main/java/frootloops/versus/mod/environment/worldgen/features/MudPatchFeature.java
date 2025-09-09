package frootloops.versus.mod.environment.worldgen.features;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class MudPatchFeature extends Feature<MudPatchFeatureConfig> {
    private static final ImmutableList<Block> CAN_REPLACE_BLOCKS = ImmutableList.of(
            Blocks.STONE, Blocks.DEEPSLATE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.CLAY, Blocks.MUD, Blocks.PACKED_MUD, CustomBlocks.GRAY_CLAY, CustomBlocks.BROWN_CLAY
    );
    private static final ImmutableList<Block> BLOCKS_ALWAYS_GRAY = ImmutableList.of(
            Blocks.STONE, Blocks.DEEPSLATE
    );

    private static final Direction[] DIRECTIONS = new Direction[]{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST};
    private static final BlockState BROWN_MUD = CustomBlocks.BROWN_MUD.getDefaultState(), BROWN_MUD_DRY = CustomBlocks.BROWN_MUD.getDryVersion().getDefaultState(), BROWN_MUD_COOKED = CustomBlocks.BROWN_MUD.getCookedVersion().getDefaultState();
    private static final BlockState GRAY_MUD = CustomBlocks.GRAY_MUD.getDefaultState(), GRAY_MUD_DRY = CustomBlocks.GRAY_MUD.getDryVersion().getDefaultState(), GRAY_MUD_COOKED = CustomBlocks.GRAY_MUD.getCookedVersion().getDefaultState();

    public MudPatchFeature(Codec<MudPatchFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<MudPatchFeatureConfig> context) {
        boolean wasAbleToGenerate = false;
        Random random = context.getRandom();
        StructureWorldAccess world = context.getWorld();
        MudPatchFeatureConfig config = context.getConfig();
        BlockPos centerPos = context.getOrigin();

        BlockState wetBlock = config.isBrownMud() ? BROWN_MUD : GRAY_MUD;
        BlockState dryBlock = config.isBrownMud() ? BROWN_MUD_DRY : GRAY_MUD_DRY;
        BlockState cookedBlock = config.isBrownMud() ? BROWN_MUD_COOKED : GRAY_MUD_COOKED;

        int size = config.size().get(random);
        double sizeSquared = (double) (size * size);
        double centerX = centerPos.getX() + 0.5;
        double centerZ = centerPos.getZ() + 0.5;
        while(world.getBlockState(centerPos).isAir()) centerPos = centerPos.down();
        while(!world.getBlockState(centerPos.up()).isAir()) centerPos = centerPos.up();

        for (BlockPos blockPos : BlockPos.iterateOutwards(centerPos, size, 0, size)) {
            double distX = blockPos.getX() - centerX;
            double distZ = blockPos.getZ() - centerZ;
            double squaredDist = distX * distX + distZ * distZ;
            if(squaredDist > sizeSquared) continue;

            // Get the correct Y level to place at. If not valid, skip it;
            BlockState blockState = world.getBlockState(blockPos);
            if(blockState.isReplaceable()) blockPos = blockPos.down();
            else if(!world.getBlockState(blockPos.up()).isReplaceable()) blockPos = blockPos.up();
            if(!world.getBlockState(blockPos.up()).isReplaceable() || !CAN_REPLACE_BLOCKS.contains(blockState.getBlock())) continue;

            // Random chance to stop if near edge:
            boolean isNearCenter = squaredDist/sizeSquared < 0.5;
            if(!isNearCenter && random.nextDouble() > squaredDist/sizeSquared) continue;

            // Get the correct block type for position:
            BlockState toPlace;
            if(config.isBrownMud() && BLOCKS_ALWAYS_GRAY.contains(blockState.getBlock())) toPlace = getBlockToPlace(world, blockPos, random, isNearCenter, GRAY_MUD, GRAY_MUD_DRY, GRAY_MUD_COOKED);
            else toPlace = getBlockToPlace(world, blockPos, random, isNearCenter, wetBlock, dryBlock, cookedBlock);
            if(toPlace == wetBlock && !isNearCenter && random.nextDouble() > squaredDist/sizeSquared) toPlace = dryBlock;
            if(toPlace == null || blockState.isOf(toPlace.getBlock())) continue;

            // Place block:
            world.setBlockState(blockPos, toPlace, Block.NOTIFY_LISTENERS);
            if(toPlace.isOf(Blocks.WATER)) world.scheduleFluidTick(blockPos, toPlace.getFluidState().getFluid(), 0);
            wasAbleToGenerate = true;

            // Next, if were in the middle of the pool:
            if(isNearCenter && (toPlace == wetBlock || toPlace.isOf(Blocks.WATER))) {

                // Random chance of placing sugar cane, if sunlit:
                if(toPlace == BROWN_MUD && world.getBlockState(blockPos.up()).isAir() && world.getLightLevel(LightType.SKY, blockPos.up()) > 14 && random.nextInt(4) == 0) {
                    world.setBlockState(blockPos.up(), Blocks.SUGAR_CANE.getDefaultState(), Block.NOTIFY_LISTENERS);
                }

                // Try placing a block below as well:
                blockPos = blockPos.down();
                blockState = world.getBlockState(blockPos);
                if(!world.getBlockState(blockPos.up()).isReplaceable() || !CAN_REPLACE_BLOCKS.contains(blockState.getBlock())) continue;
                toPlace = config.isBrownMud() && BLOCKS_ALWAYS_GRAY.contains(blockState.getBlock()) ? GRAY_MUD : wetBlock;

                // Place block below:
                world.setBlockState(blockPos, toPlace, Block.NOTIFY_LISTENERS);
                if(toPlace.isOf(Blocks.WATER)) world.scheduleFluidTick(blockPos, toPlace.getFluidState().getFluid(), 0);
            }
        }
        return wasAbleToGenerate;
    }

    private static BlockState getBlockToPlace(WorldAccess world, BlockPos pos, Random random, boolean isNearCenter, BlockState wetBlock, BlockState dryBlock, BlockState cookedBlock) {
        BlockState neighborState = world.getBlockState(pos.up());
        if(!neighborState.getFluidState().isEmpty()) {
            if(neighborState.getFluidState().isIn(FluidTags.LAVA)) return cookedBlock;
            else if(neighborState.getFluidState().isIn(FluidTags.WATER)) return isNearCenter && !world.getBlockState(pos.down()).isAir() ? Blocks.WATER.getDefaultState() : wetBlock;
        }
        else if(neighborState.isOf(wetBlock.getBlock()) && neighborState.isOf(Blocks.DIRT)) return wetBlock;

        BlockState stateToReturn = wetBlock;
        int numAirBlocks = 0;
        for (Direction direction : DIRECTIONS) {
            neighborState = world.getBlockState(pos.offset(direction));
            if(!neighborState.getFluidState().isEmpty()) {
                if(neighborState.getFluidState().isIn(FluidTags.LAVA)) return cookedBlock;
                else if(neighborState.getFluidState().isIn(FluidTags.WATER)) return isNearCenter && random.nextBoolean() && !world.getBlockState(pos.down()).isAir() ? Blocks.WATER.getDefaultState() : wetBlock;
            }
            else if (neighborState.isReplaceable()) {
                stateToReturn = dryBlock;
                numAirBlocks++;
            }
        }
        if(isNearCenter && stateToReturn == wetBlock && random.nextFloat() < 0.1f && !world.getBlockState(pos.down()).isAir()) return Blocks.WATER.getDefaultState();
        else return numAirBlocks == 4 ? null : stateToReturn;
    }
}
