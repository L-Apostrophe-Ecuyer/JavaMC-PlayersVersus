package frootloops.versus.mixin.environment;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.block.enums.Thickness;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneMixin extends Block {
    @Shadow public static final DirectionProperty VERTICAL_DIRECTION = Properties.VERTICAL_DIRECTION;

    private static final Map<Block,Block> BLOCKS_THAT_DRIP_WATER = new HashMap<>();
    static {
        BLOCKS_THAT_DRIP_WATER.put(Blocks.MUD, Blocks.CLAY);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_MUD, Blocks.PACKED_MUD);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_MUD_BRICKS, Blocks.MUD_BRICKS);
        BLOCKS_THAT_DRIP_WATER.put(CustomBlocks.BROWN_MUD_TILES, CustomBlocks.PACKED_MUD_TILES);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.PACKED_MUD, Blocks.CLAY);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.FARMLAND, Blocks.DIRT);
        BLOCKS_THAT_DRIP_WATER.put(Blocks.DIRT, Blocks.COARSE_DIRT);
    }

    public PointedDripstoneMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
        if (random.nextBoolean()) return;
        if (state.get(VERTICAL_DIRECTION) != Direction.DOWN) return;
        dripTickOverhauled(state, world, pos, random);
        info.cancel();
    }


    private static void dripTickOverhauled(BlockState state, ServerWorld world, BlockPos pos, Random random) {

        Fluid fluid = null;
        BlockPos.Mutable mutableBlockPos = pos.mutableCopy();
        BlockState mutableBlockState = null;
        for (int i = 1; i < 11; ++i) {
            mutableBlockPos.move(Direction.UP);
            if(world.getTopY() >= mutableBlockPos.getY()) return; // No fluid here.

            mutableBlockState = world.getBlockState(mutableBlockPos);
            if(mutableBlockState.isOf(Blocks.DRIPSTONE_BLOCK) || (mutableBlockState.isOf(Blocks.POINTED_DRIPSTONE) && mutableBlockState.get(VERTICAL_DIRECTION) == Direction.UP)) continue;
            break;
        }

        boolean isUltrawarm = world.getDimension().ultrawarm();
        if(isUltrawarm && mutableBlockState.isOf(Blocks.MAGMA_BLOCK)) {
            fluid = Fluids.LAVA;
        }
        else if(!world.getDimension().ultrawarm() && BLOCKS_THAT_DRIP_WATER.containsKey(mutableBlockState.getBlock())) {
            BlockState resultBlockState = BLOCKS_THAT_DRIP_WATER.get(mutableBlockState.getBlock()).getStateWithProperties(mutableBlockState);
            world.setBlockState(mutableBlockPos, resultBlockState);
            Block.pushEntitiesUpBeforeBlockChange(mutableBlockState, resultBlockState, world, mutableBlockPos);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, mutableBlockPos, GameEvent.Emitter.of(resultBlockState));
            world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS, pos, 0);
            fluid = Fluids.WATER;
        }
        else {
            fluid = world.getFluidState(mutableBlockPos).getFluid();
            if(fluid == Fluids.FLOWING_LAVA && isUltrawarm) fluid = Fluids.LAVA;
        }


        if (fluid == Fluids.WATER) {
            if (random.nextBoolean()) PointedDripstoneBlock.tryGrow(state, world, pos, random);
        }
        else if (fluid == Fluids.LAVA) {
            if (random.nextBoolean()) return;
        }
        else return;


        // Check if we can make something down below wet:
        for (int i = 1; i < 11; ++i) {
            mutableBlockPos.move(Direction.DOWN);
            if(world.getBottomY() <= mutableBlockPos.getY()) return; // No fluid here.

            mutableBlockState = world.getBlockState(mutableBlockPos);
            if(mutableBlockState.isAir()) continue;
            if(!mutableBlockState.getFluidState().isEmpty()) break; // Already fluid down below
            if((mutableBlockState.isOf(Blocks.POINTED_DRIPSTONE) && mutableBlockState.get(VERTICAL_DIRECTION) == Direction.DOWN)) continue;
            break;
        }

        Optional<Integer> optionalMoisture;
        if(mutableBlockState.isOf(Blocks.CAULDRON)) {
            if (fluid == Fluids.WATER) {
                world.setBlockState(mutableBlockPos, Blocks.WATER_CAULDRON.getDefaultState());
                world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
            }
            else if (fluid == Fluids.LAVA) {
                world.setBlockState(mutableBlockPos, Blocks.LAVA_CAULDRON.getDefaultState());
                world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
            }
        }
        else if(mutableBlockState.isOf(Blocks.WATER_CAULDRON) && fluid == Fluids.WATER) {
            int currentLevel = mutableBlockState.get(Properties.LEVEL_3);
            if (currentLevel < 3) {
                world.setBlockState(mutableBlockPos, mutableBlockState.with(Properties.LEVEL_3, currentLevel + 1));
                world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
            }
        }
        else if((optionalMoisture = mutableBlockState.getOrEmpty(Properties.MOISTURE)).isPresent()) {
            int currentMoisture = optionalMoisture.get();
            if(currentMoisture < 5) {
                world.setBlockState(mutableBlockPos, mutableBlockState.with(Properties.MOISTURE, 5));
                world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
            }
        }
    }
}
