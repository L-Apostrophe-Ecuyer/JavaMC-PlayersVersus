package frootloops.versus.mod.environment.blocks.clays;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;


public interface MoistureConvertableBlock {

    static final float MIN_FALL_DISTANCE_TO_DRY = 5.0f;

    static final int MOISTURE_LVL_TO_COOK = -16;
    static final int MOISTURE_LVL_TO_DRY = -4;
    static final int MOISTURE_LVL_TO_WET = 4;

    static final int LAVA_MOISTURE = MOISTURE_LVL_TO_COOK;
    static final int FIRE_MOISTURE = MOISTURE_LVL_TO_COOK/2;
    static final int DRY_BLOCK_MOISTURE = MOISTURE_LVL_TO_DRY;
    static final int WET_BLOCK_MOISTURE = MOISTURE_LVL_TO_WET/4;
    static final int WATER_MOISTURE = MOISTURE_LVL_TO_WET + 2;


    public abstract Block getDryVersion();
    public abstract Block getCookedVersion();
    public abstract boolean hasWetVersion();

    static void scheduledTick(BlockState state, ServerLevel world, BlockPos pos, Block dryBlock, Block cookedBlock, Block wetBlock) {
        if(world.isRainingAt(pos.above())) {
            if(wetBlock != null && getMoistureAmountOf(world, pos, state) > 0) world.setBlockAndUpdate(pos, wetBlock.withPropertiesOf(state));
        }
        else {
            int moisture = getMoistureAmountOf(world, pos, state);
            if(moisture < MOISTURE_LVL_TO_COOK) world.setBlockAndUpdate(pos, cookedBlock.withPropertiesOf(state));
            else if(moisture < MOISTURE_LVL_TO_DRY) if(dryBlock != null) world.setBlockAndUpdate(pos, dryBlock.withPropertiesOf(state));
            else if(moisture > MOISTURE_LVL_TO_WET && wetBlock != null) world.setBlockAndUpdate(pos, wetBlock.withPropertiesOf(state));
        }
    }

    static void onLandedUpon(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance, Block dryBlock) {
        if(dryBlock == null) return;
        if(entity instanceof FallingBlockEntity) {
            BlockState result = dryBlock.withPropertiesOf(state);
            world.setBlockAndUpdate(pos, result);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, result));
        }
        else if(entity instanceof LivingEntity && fallDistance > MIN_FALL_DISTANCE_TO_DRY) {
            if(fallDistance < 20f && entity.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;
            BlockState blockState = dryBlock.withPropertiesOf(state);
            world.setBlockAndUpdate(pos, blockState);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, blockState));
        }
    }

    private static int getMoistureAmountOf(LevelReader world, BlockPos pos, BlockState state) {
        if(state.getFluidState().is(Fluids.WATER)) return 4;
        return getMoistureAmountAround(world,pos);
    }

    private static int getMoistureAmountAround(LevelReader world, BlockPos pos) {
        BlockState blockStateDown = world.getBlockState(pos.below());
        int moistureDown = Math.min(1, getBlockMoisture(blockStateDown));
        if(moistureDown <= MOISTURE_LVL_TO_DRY) return moistureDown - 1;

        BlockState blockStateDownDown = world.getBlockState(pos.below().below());
        if((blockStateDown.is(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty()) || (blockStateDownDown.is(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty())) return MOISTURE_LVL_TO_DRY;

        int moistureUp = world.getMaxLocalRawBrightness(pos) > 12 ? DRY_BLOCK_MOISTURE : 0;
        int moistureAmount = moistureDown + moistureUp + (world.dimensionType().ultraWarm() ? DRY_BLOCK_MOISTURE : 0);
        BlockState neighborState;
        BlockPos[] neighborsPos = new BlockPos[] {pos.above(), pos.north(), pos.south(), pos.west(), pos.east()};
        for (BlockPos blockPos : neighborsPos) {
            neighborState = world.getBlockState(blockPos);
            moistureAmount += getBlockMoisture(neighborState);
            if(moistureAmount <= MOISTURE_LVL_TO_COOK) return MOISTURE_LVL_TO_COOK;
        }
        return moistureAmount;
    }

    private static int getBlockMoisture(BlockState state) {
        if(state.isAir()) return -1;
        if(state.is(Blocks.LAVA)) return LAVA_MOISTURE;
        if(state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)) return FIRE_MOISTURE;
        if(state.is(Blocks.TORCH) || state.is(Blocks.SOUL_TORCH)) return DRY_BLOCK_MOISTURE;
        if((state.is(BlockTags.CAMPFIRES) || state.getBlock() instanceof AbstractFurnaceBlock) && state.getValue(BlockStateProperties.LIT)) return FIRE_MOISTURE + 1;
        if(state.is(Blocks.SPONGE)) return DRY_BLOCK_MOISTURE;
        if(state.is(Blocks.WET_SPONGE)) return WATER_MOISTURE;
        if(state.getFluidState().is(FluidTags.WATER)) return WATER_MOISTURE;
        if(state instanceof MoistureConvertableBlock moistBlock && moistBlock.hasWetVersion()) return WET_BLOCK_MOISTURE;
        return 0;
    }
}
