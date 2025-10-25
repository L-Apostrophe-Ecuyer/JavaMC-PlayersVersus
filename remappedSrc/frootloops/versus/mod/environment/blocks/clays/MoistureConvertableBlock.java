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
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;


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

    static void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Block dryBlock, Block cookedBlock, Block wetBlock) {
        if(world.hasRain(pos.up())) {
            if(wetBlock != null && getMoistureAmountOf(world, pos, state) > 0) world.setBlockState(pos, wetBlock.getStateWithProperties(state));
        }
        else {
            int moisture = getMoistureAmountOf(world, pos, state);
            if(moisture < MOISTURE_LVL_TO_COOK) world.setBlockState(pos, cookedBlock.getStateWithProperties(state));
            else if(moisture < MOISTURE_LVL_TO_DRY) if(dryBlock != null) world.setBlockState(pos, dryBlock.getStateWithProperties(state));
            else if(moisture > MOISTURE_LVL_TO_WET && wetBlock != null) world.setBlockState(pos, wetBlock.getStateWithProperties(state));
        }
    }

    static void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance, Block dryBlock) {
        if(dryBlock == null) return;
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
        if(moistureDown <= MOISTURE_LVL_TO_DRY) return moistureDown - 1;

        BlockState blockStateDownDown = world.getBlockState(pos.down().down());
        if((blockStateDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty()) || (blockStateDownDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty())) return MOISTURE_LVL_TO_DRY;

        int moistureUp = world.getLightLevel(pos) > 12 ? DRY_BLOCK_MOISTURE : 0;
        int moistureAmount = moistureDown + moistureUp + (world.getDimension().ultrawarm() ? DRY_BLOCK_MOISTURE : 0);
        BlockState neighborState;
        BlockPos[] neighborsPos = new BlockPos[] {pos.up(), pos.north(), pos.south(), pos.west(), pos.east()};
        for (BlockPos blockPos : neighborsPos) {
            neighborState = world.getBlockState(blockPos);
            moistureAmount += getBlockMoisture(neighborState);
            if(moistureAmount <= MOISTURE_LVL_TO_COOK) return MOISTURE_LVL_TO_COOK;
        }
        return moistureAmount;
    }

    private static int getBlockMoisture(BlockState state) {
        if(state.isAir()) return -1;
        if(state.isOf(Blocks.LAVA)) return LAVA_MOISTURE;
        if(state.isIn(BlockTags.FIRE) || state.isOf(Blocks.MAGMA_BLOCK)) return FIRE_MOISTURE;
        if(state.isOf(Blocks.TORCH) || state.isOf(Blocks.SOUL_TORCH)) return DRY_BLOCK_MOISTURE;
        if((state.isIn(BlockTags.CAMPFIRES) || state.getBlock() instanceof AbstractFurnaceBlock) && state.get(Properties.LIT)) return FIRE_MOISTURE + 1;
        if(state.isOf(Blocks.SPONGE)) return DRY_BLOCK_MOISTURE;
        if(state.isOf(Blocks.WET_SPONGE)) return WATER_MOISTURE;
        if(state.getFluidState().isIn(FluidTags.WATER)) return WATER_MOISTURE;
        if(state instanceof MoistureConvertableBlock moistBlock && moistBlock.hasWetVersion()) return WET_BLOCK_MOISTURE;
        return 0;
    }
}
