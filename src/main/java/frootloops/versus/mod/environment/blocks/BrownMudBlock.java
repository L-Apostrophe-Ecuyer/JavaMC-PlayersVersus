package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mixin.LivingEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.NavigationType;
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

    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

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
        int i = state.get(MOISTURE);
        if (isWaterNearby(world, pos) || world.hasRain(pos.up())) {
            if (i < 4) world.setBlockState(pos, (BlockState)state.with(MOISTURE, 4), Block.NOTIFY_LISTENERS);
        } else if (i > 0) {
            world.setBlockState(pos, (BlockState)state.with(MOISTURE, i - 1), Block.NOTIFY_LISTENERS);
        }
    }

    private static boolean isWaterNearby(WorldView world, BlockPos pos) {
        Optional<Integer> moisture;
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
            moisture = world.getBlockState(blockPos).getOrEmpty(MOISTURE);
            if (moisture.isPresent() && moisture.get() > 0) return true;
            if (world.getFluidState(blockPos).isIn(FluidTags.WATER)) return true;
        }
        return false;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && entity.getBlockStateAtPos().isOf(this) && !canWalkOnWetMud(entity)) {
            Vec3d velocity = entity.getVelocity();
            if(velocity.y < 0.05) {
                entity.setVelocity(velocity.multiply(1.0, 0.3, 1.0));
            }
            if(entity instanceof LivingEntityAccessor livingEntityAccessor) {
                if(velocity.y > 0.0 || livingEntityAccessor.isJumping()) {
                    entity.slowMovement(state, new Vec3d(1.1, 1.0, 1.1)); // Resets movement multiplier
                    entity.setVelocity(entity.getVelocity().x, 0.28, entity.getVelocity().z);
                }
                else if(hasEntityMoved(entity)) entity.slowMovement(state, new Vec3d(1.0, 0.8, 1.0));
                else entity.slowMovement(state, new Vec3d(1.0, 0.1, 1.0));
            }
            if (hasEntityMoved(entity) || world.getRandom().nextFloat() < 0.2) {
                if (entity instanceof LivingEntity livingEntity && shouldDamage(world, livingEntity)) {
                    livingEntity.damage(livingEntity.getDamageSources().inWall(), 0.5f);
                }
            }
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (fallDistance < 4.0f || (!(entity instanceof LivingEntity) && !(entity instanceof FallingBlockEntity))) return;
        if(entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;

        BlockState blockState;
        int moisture = state.get(MOISTURE);
        if(moisture == 0 && fallDistance > 16.0f) blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.PACKED_MUD.getDefaultState(), world, pos);
        else if(moisture > 0 && fallDistance < 8.0f) blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.FARMLAND.getDefaultState(), world, pos);
        else if(moisture > 0) return;
        else blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.DIRT.getDefaultState(), world, pos);
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(MOISTURE) == 0) return SHAPE;

        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext)context).getEntity()) != null) {
            if (entity.fallDistance > 2.5f || BrownMudBlock.canWalkOnWetMud(entity) || entity instanceof FallingBlockEntity) return SHAPE;
            return VoxelShapes.empty();
        }
        return SHAPE;
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
        if (entity instanceof ItemEntity || entity instanceof PigEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
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
