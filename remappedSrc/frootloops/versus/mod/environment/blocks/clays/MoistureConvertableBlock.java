package frootloops.versus.mod.environment.blocks.clays;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;


public interface MoistureConvertableBlock {

    static final float MIN_FALL_DISTANCE_TO_DRY = 5.0f;

    static void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Block dryBlock, Block cookedBlock, Block wetBlock) {
        if(world.hasRain(pos.up())) {
            if(wetBlock != null && getMoistureAmountOf(world, pos, state) > 0) world.setBlockState(pos, wetBlock.getStateWithProperties(state));
        }
        else {
            int moisture = getMoistureAmountOf(world, pos, state);
            if(moisture < -6) world.setBlockState(pos, cookedBlock.getStateWithProperties(state));
            else if(moisture < -1) world.setBlockState(pos, dryBlock.getStateWithProperties(state));
            else if(moisture > 2) {
                if(moisture > 2 && wetBlock != null) world.setBlockState(pos, wetBlock.getStateWithProperties(state));
            }
        }
    }

    static void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, Block dryBlock) {
        if(entity instanceof FallingBlockEntity) {
            BlockState result = dryBlock.getStateWithProperties(state);
            world.setBlockState(pos, result);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, result));
        }
        else if(entity instanceof LivingEntity && fallDistance > MIN_FALL_DISTANCE_TO_DRY) {
            if(fallDistance < 20f && entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;
            BlockState blockState = dryBlock.getStateWithProperties(state);
            world.setBlockState(pos, blockState);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
        }
    }

    private static int getMoistureAmountOf(WorldView world, BlockPos pos, BlockState state) {
        if(state.getFluidState().isOf(Fluids.WATER)) return 4;
        return getMoistureAmountAround(world,pos);
    }

    private static int getMoistureAmountAround(WorldView world, BlockPos pos) {
        BlockState blockStateDown = world.getBlockState(pos.down());
        int moistureDown = Math.min(1, getBlockMoisture(blockStateDown));
        if(moistureDown < -4) return -4;

        BlockState blockStateDownDown = world.getBlockState(pos.down().down());
        if((blockStateDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty()) || (blockStateDownDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty())) return -4;

        int moistureAmount = moistureDown + (world.getDimension().ultrawarm() ? -3 : (blockStateDown.isAir() ? -1 : 0));
        BlockState neighborState;
        BlockPos[] neighborsPos = new BlockPos[] {pos.up(), pos.north(), pos.south(), pos.west(), pos.east()};
        for (BlockPos blockPos : neighborsPos) {
            neighborState = world.getBlockState(blockPos);
            moistureAmount += getBlockMoisture(neighborState);
            if(moistureAmount < -6) return -6;
        }
        return moistureAmount;
    }

    private static int getBlockMoisture(BlockState state) {
        if(state.isOf(Blocks.LAVA)) return -8;
        if(state.isIn(BlockTags.FIRE)) return -4;
        if(state.isIn(BlockTags.CAMPFIRES) && state.get(Properties.LIT)) return -3;
        if(state.getBlock() instanceof AbstractFurnaceBlock && state.get(Properties.LIT)) return -2;
        if(state.isOf(Blocks.SPONGE)) return -1;
        if(state.isOf(Blocks.WET_SPONGE)) return 1;
        if(state.getFluidState().isIn(FluidTags.WATER)) return 2;
        return 0;
    }
}
