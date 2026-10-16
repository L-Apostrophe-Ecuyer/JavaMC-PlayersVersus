package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.WorldTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneMixin extends Block {
    private static final Map<Block,Block> BLOCKS_THAT_DRIP_WATER = new HashMap<>();
    static {
        BLOCKS_THAT_DRIP_WATER.put(Blocks.MUD, CustomBlocks.GRAY_CLAY);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_MUD, CustomBlocks.BROWN_CLAY);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_MUD_BRICKS, CustomBlocks.BROWN_CLAY_BRICKS);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_CLAY, Blocks.DRIPSTONE_BLOCK);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.ANDESITE, Blocks.DRIPSTONE_BLOCK);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.PACKED_MUD, Blocks.DRIPSTONE_BLOCK);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.FARMLAND, Blocks.DIRT);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.DIRT, Blocks.COARSE_DIRT);
    }

    public PointedDripstoneMixin(Properties settings) {
        super(settings);
    }

    @Overwrite
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (random.nextBoolean()) return;
        if (state.getValue(PointedDripstoneBlock.TIP_DIRECTION) != Direction.DOWN) return;
        dripTickOverhauled(state, world, pos, random);
    }


    private static void dripTickOverhauled(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {

        Fluid fluid;
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        BlockState mutableBlockState = null;
        int maxWorldHeight = world.dimensionType().logicalHeight();

        for (int i = 1; i < 11; ++i) {
            mutableBlockPos.move(Direction.UP);
            if(maxWorldHeight >= mutableBlockPos.getY()) return; // No fluid here.

            mutableBlockState = world.getBlockState(mutableBlockPos);
            if(mutableBlockState.is(Blocks.DRIPSTONE_BLOCK) || (mutableBlockState.is(Blocks.POINTED_DRIPSTONE) && mutableBlockState.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.UP)) continue;
            break;
        }

        boolean isUltrawarm = WorldTime.ultraWarm(world);
        if(isUltrawarm && mutableBlockState.is(Blocks.MAGMA_BLOCK)) {
            fluid = Fluids.LAVA;
        }
        else if(!isUltrawarm && BLOCKS_THAT_DRIP_WATER.containsKey(mutableBlockState.getBlock())) {
            BlockState resultBlockState = BLOCKS_THAT_DRIP_WATER.get(mutableBlockState.getBlock()).withPropertiesOf(mutableBlockState);
            world.setBlockAndUpdate(mutableBlockPos, resultBlockState);
            Block.pushEntitiesUp(mutableBlockState, resultBlockState, world, mutableBlockPos);
            world.gameEvent(GameEvent.BLOCK_CHANGE, mutableBlockPos, GameEvent.Context.of(resultBlockState));
            world.levelEvent(LevelEvent.DRIPSTONE_DRIP, pos, 0);
            fluid = Fluids.WATER;
        }
        else {
            fluid = world.getFluidState(mutableBlockPos).getType();
            if(fluid == Fluids.FLOWING_LAVA && isUltrawarm) fluid = Fluids.LAVA;
        }


        // Try to grow the stalagmite:
        if (fluid == Fluids.WATER) {
            // An instance method of SpeleothemBlock since 26.3, which PointedDripstoneBlock extends.
            if (random.nextBoolean()) ((PointedDripstoneBlock) state.getBlock()).growStalactiteOrStalagmiteIfPossible(state, world, pos, random);
        }

        // Possible skip if lava:
        else if (fluid == Fluids.LAVA) {
            if (random.nextBoolean()) return;
        }
        else return;


        // Check if we can make something down below wet:
        for (int i = 1; i < 11; ++i) {
            mutableBlockPos.move(Direction.DOWN);
            if(world.getMinY() <= mutableBlockPos.getY()) return; // No fluid here.

            mutableBlockState = world.getBlockState(mutableBlockPos);
            if(mutableBlockState.isAir()) continue;
            if(!mutableBlockState.getFluidState().isEmpty()) break; // Already fluid down below
            if((mutableBlockState.is(Blocks.POINTED_DRIPSTONE) && mutableBlockState.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.DOWN)) continue;
            break;
        }

        Optional<Integer> optionalMoisture;
        if(mutableBlockState.is(Blocks.CAULDRON)) {
            if (fluid == Fluids.WATER) {
                world.setBlockAndUpdate(mutableBlockPos, Blocks.WATER_CAULDRON.defaultBlockState());
                world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
            }
            else if (fluid == Fluids.LAVA) {
                world.setBlockAndUpdate(mutableBlockPos, Blocks.LAVA_CAULDRON.defaultBlockState());
                world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
            }
        }
        else if(mutableBlockState.is(Blocks.WATER_CAULDRON) && fluid == Fluids.WATER) {
            int currentLevel = mutableBlockState.getValue(BlockStateProperties.LEVEL_CAULDRON);
            if (currentLevel < 3) {
                world.setBlockAndUpdate(mutableBlockPos, mutableBlockState.setValue(BlockStateProperties.LEVEL_CAULDRON, currentLevel + 1));
                world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
            }
        }
        else if((optionalMoisture = mutableBlockState.getOptionalValue(BlockStateProperties.MOISTURE)).isPresent()) {
            int currentMoisture = optionalMoisture.get();
            if(currentMoisture < 5) {
                world.setBlockAndUpdate(mutableBlockPos, mutableBlockState.setValue(BlockStateProperties.MOISTURE, 5));
                world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
            }
        }
    }
}
