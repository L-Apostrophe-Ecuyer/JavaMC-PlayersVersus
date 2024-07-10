package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class BrownMudBlock extends FarmlandBlock {
    public BrownMudBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.up());
        return !blockState.isSolid() || blockState.isOf(this);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            FarmlandBlock.setToDirt(null, state, world, pos);
        }
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }


    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int currentMoisture = state.get(MOISTURE);
        int nearbyMoisture = world.getDimension().ultrawarm() ? -4 : world.hasRain(pos.up()) ? 4 : getMoistureAmountNearby(world, pos);
        if (nearbyMoisture > 0) {
            if (currentMoisture < 4) world.setBlockState(pos, state.with(MOISTURE, Math.min(4, currentMoisture + nearbyMoisture)), Block.NOTIFY_LISTENERS);
        } else if (nearbyMoisture == -4 && currentMoisture == 0) {
            world.setBlockState(pos, Blocks.PACKED_MUD.getDefaultState(), Block.NOTIFY_LISTENERS);
        } else if (currentMoisture > 0) {
            world.setBlockState(pos, state.with(MOISTURE, currentMoisture - 1), Block.NOTIFY_LISTENERS);
        }
    }

    private static int getMoistureAmountNearby(WorldView world, BlockPos pos) {
        Optional<Integer> moisture;
        BlockState state;
        int moistureAmount = -1;
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
            state = world.getBlockState(blockPos);
            if (state.isOf(Blocks.FIRE)) return -4;
            if (state.getFluidState().isIn(FluidTags.WATER)) return 4;
            moisture = world.getBlockState(blockPos).getOrEmpty(MOISTURE);
            if (moisture.isPresent() && moisture.get() > 0) moistureAmount += moisture.get();
            if (moistureAmount == 4) return 4;
        }
        return moistureAmount;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && entity.getBlockStateAtPos().isOf(this) && !canWalkOnWetMud(entity)) {
            Vec3d velocity = entity.getVelocity();
            if(velocity.y < 0.05) {
                entity.setVelocity(velocity.multiply(1.0, 0.3, 1.0));
            }
            if(entity instanceof LivingEntityAccessor livingEntityAccessor) {
                if(entity.getY() - Math.floor(entity.getY()) < 0.5 && world.getBlockState(pos.up()).getBlock() instanceof PlantBlock) {
                    world.breakBlock(pos.up(), true);
                }
                if(livingEntityAccessor.isJumping())  {
                    if(entity.isInFluid()) {
                        entity.slowMovement(state, new Vec3d(1.1, 1.0, 1.1));
                        entity.setVelocity(velocity.add(0.0, 0.05, 0.0));
                    }
                    else {
                        entity.slowMovement(state, new Vec3d(1.1, 0.0, 1.1));
                    }
                }
                else if(hasEntityMoved(entity)) {
                    entity.slowMovement(state, new Vec3d(0.95, 0.5, 0.95));
                }
                else {
                    entity.slowMovement(state, new Vec3d(0.95, 0.05, 0.95));
                }
            }
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (fallDistance < 4.0f || (!(entity instanceof LivingEntity) && !(entity instanceof FallingBlockEntity))) return;
        if(entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;

        BlockState blockState;
        if(state.get(MOISTURE) == 0) {
            if( fallDistance > 16.0f) blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.PACKED_MUD.getDefaultState(), world, pos);
            else blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.DIRT.getDefaultState(), world, pos);
        }
        else {
            if(fallDistance < 8.0f) blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.FARMLAND.getDefaultState(), world, pos);
            else return;
        }
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(MOISTURE) == 0) return VoxelShapes.fullCube();

        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext)context).getEntity()) != null) {
            if (entity.fallDistance > 2.5f || BrownMudBlock.canWalkOnWetMud(entity) || entity instanceof FallingBlockEntity) return VoxelShapes.fullCube();
            return VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }


    public boolean shouldDamage(World world, LivingEntity entity) {
        if(entity instanceof PigEntity) return false;
        if(world.getTime() % 80L != 0) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.11), entity.getBlockZ())).isOf(this);
    }

    public boolean hasEntityMoved(Entity entity) {
        return entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ() ||
                entity.prevYaw != entity.getYaw() || entity.prevPitch != entity.getPitch();
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof WaterCreatureEntity || entity instanceof PigEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return (state.get(MOISTURE) > 0);
    }

    @Override
    protected VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected boolean hasSidedTransparency(BlockState state) {
        return false;
    }

    @Override
    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 0.2f;
    }

}
