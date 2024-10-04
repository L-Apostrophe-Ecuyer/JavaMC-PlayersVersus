package frootloops.versus.mod.environment.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public interface PackedMudConvertableBlock {

    static void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Block dryBlock) {
        if(world.getDimension().ultrawarm() || (!world.hasRain(pos.up()) && getMoistureAmountNearby(world, pos) < 0)){
            world.setBlockState(pos, dryBlock.getStateWithProperties(state));
        }
    }

    static void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, Block dryBlock) {
        if (fallDistance < 7.0f || (!(entity instanceof LivingEntity) && !(entity instanceof FallingBlockEntity))) return;
        if(entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;

        BlockState resultingState = dryBlock.getStateWithProperties(state);
        world.setBlockState(pos, resultingState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, resultingState));
    }

    private static int getMoistureAmountNearby(WorldView world, BlockPos pos) {
        BlockState blockStateDown = world.getBlockState(pos.down());
        if(blockStateDown.isIn(BlockTags.FIRE) || blockStateDown.isIn(BlockTags.CAMPFIRES) || blockStateDown.isOf(Blocks.LAVA)) return -4;

        BlockState blockStateDownDown = world.getBlockState(pos.down().down());
        if((blockStateDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty()) || (blockStateDownDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty())) return -4;

        Optional<Integer> moisture;
        int moistureAmount = blockStateDown.isAir() ? -1 : 0;

        BlockState neighborState;
        BlockPos[] neighborsPos = new BlockPos[] {pos.up(), pos.north(), pos.south(), pos.west(), pos.east()};
        for (BlockPos blockPos : neighborsPos) {
            neighborState = world.getBlockState(blockPos);
            if(blockStateDown.isIn(BlockTags.FIRE) || blockStateDown.isIn(BlockTags.CAMPFIRES) || blockStateDown.isOf(Blocks.LAVA)) return -4;
            if (neighborState.getFluidState().isIn(FluidTags.WATER)) return 4;

            moisture = neighborState.getOrEmpty(Properties.MOISTURE);
            if (moisture.isPresent() && moisture.get() > 0) moistureAmount += Math.min(2,  (moisture.get() + 1)/2);
        }

        return Math.min(3, Math.max(0, moistureAmount/2));
    }
}
