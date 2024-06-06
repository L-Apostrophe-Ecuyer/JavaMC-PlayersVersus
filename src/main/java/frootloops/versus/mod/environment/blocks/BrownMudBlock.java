package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mixin.LivingEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
    private static final VoxelShape FALLING_SHAPE =Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape DRY_SOLID_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

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
        if (BrownMudBlock.isWaterNearby(world, pos)) if (currentMoisture < 7) world.setBlockState(pos, state.with(MOISTURE, 7), Block.NOTIFY_LISTENERS);
        else if (currentMoisture > 0) world.setBlockState(pos, state.with(MOISTURE, currentMoisture - 1), Block.NOTIFY_LISTENERS);
    }

    private static boolean isWaterNearby(WorldView world, BlockPos pos) {
        Optional<Integer> moisture;
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-3, -1, -3), pos.add(3, 1, 3))) {
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
            double y = velocity.getY();
            if(y < 0) {
                y *= 0.3;
            }
            entity.setVelocity(new Vec3d(0, y, 0));
            if(entity instanceof LivingEntityAccessor livingEntityAccessor && livingEntityAccessor.isJumping()) {
                entity.setVelocity(entity.getVelocity().x, 1.0, entity.getVelocity().z);
            }
            else {
                entity.slowMovement(state, new Vec3d(0.3, 1.0, 0.3));
            }

            if (hasEntityMoved(entity) || world.getRandom().nextFloat() < 0.2) {
                if (entity instanceof LivingEntity livingEntity && shouldDamage(world, livingEntity)) {
                    livingEntity.damage(livingEntity.getDamageSources().inWall(), 0.5f);
                }
            }
        }
    }

    public boolean shouldDamage(World world, LivingEntity entity) {
        if(entity instanceof PigEntity) return false;
        if(world.getTime() % 80L != 0) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.11), entity.getBlockZ())).isOf(this);
    }

    public boolean hasEntityMoved(Entity entity) {
        return entity.lastRenderX != entity.getX() || entity.lastRenderY != entity.getY() || entity.lastRenderZ != entity.getZ() ||
                entity.prevYaw != entity.getYaw() || entity.prevPitch != entity.getPitch();
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (fallDistance < 4.0f || !(entity instanceof LivingEntity)) return;
        if(state.get(MOISTURE) == 0) {
            BlockState blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.DIRT.getDefaultState(), world, pos);
            world.setBlockState(pos, blockState);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
        }
        LivingEntity livingEntity = (LivingEntity) entity;
        LivingEntity.FallSounds fallSounds = livingEntity.getFallSounds();
        SoundEvent soundEvent = (double) fallDistance < 7.0 ? fallSounds.small() : fallSounds.big();
        entity.playSound(soundEvent, 1.0f, 1.0f);
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(MOISTURE) == 0) return DRY_SOLID_SHAPE;

        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext)context).getEntity()) != null) {
            if (entity.fallDistance > 2.5f) {
                return FALLING_SHAPE;
            }
            boolean bl = entity instanceof FallingBlockEntity;
            if (bl || BrownMudBlock.canWalkOnWetMud(entity) && context.isAbove(VoxelShapes.fullCube(), pos, false) && !context.isDescending()) {
                return super.getCollisionShape(state, world, pos, context);
            }
            return VoxelShapes.empty();
        }
        return FALLING_SHAPE;
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if(entity.getVelocity().y > 0.0) return true;
        if (entity instanceof ItemEntity || entity instanceof PigEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(MOISTURE) == 0) return VoxelShapes.fullCube();
        return VoxelShapes.empty();
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return (state.get(MOISTURE) > 0);
    }

    @Override
    protected VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        if(state.get(MOISTURE) == 0) return VoxelShapes.fullCube();
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (state.get(MOISTURE) > 0 && stateFrom.isOf(this)) return true;
        return super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 0.2f;
    }

}
