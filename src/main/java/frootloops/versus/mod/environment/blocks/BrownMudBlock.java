package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomDamageSources;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BrownMudBlock extends Block {

    public static final IntProperty MOISTURE = Properties.MOISTURE;
    public static final int MAX_MOISTURE = 4;
    public static final int MAX_MOISTURE_SOLID = -1;

    public BrownMudBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(MOISTURE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.up());
        if(!blockState.isSolid() || blockState.isOf(this) || blockState.isIn(BlockTags.DIRT)) return true;
        else return !(getMoistureAmountNearby(world, pos) < 0);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int currentMoisture = state.get(MOISTURE);
        int nearbyMoisture = world.getDimension().ultrawarm() ? -MAX_MOISTURE : world.hasRain(pos.up()) ? MAX_MOISTURE : getMoistureAmountNearby(world, pos);
        if(currentMoisture != nearbyMoisture) world.setBlockState(pos, state.with(MOISTURE, Math.max(0, Math.min(MAX_MOISTURE, nearbyMoisture))), Block.NOTIFY_LISTENERS);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }


    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(world.getDimension().ultrawarm()) {
            world.setBlockState(pos, Blocks.PACKED_MUD.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }
        BlockState blockStateDown = world.getBlockState(pos.down());
        int currentMoisture = state.get(MOISTURE);
        int nearbyMoisture = world.hasRain(pos.up()) ? MAX_MOISTURE : getMoistureAmountNearby(world, pos, blockStateDown);

        if (nearbyMoisture < 0) {
            if(currentMoisture == 0) world.setBlockState(pos, Blocks.PACKED_MUD.getDefaultState(), Block.NOTIFY_LISTENERS);
            else world.setBlockState(pos, this.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        else {
            BlockState blockStateDownDown = world.getBlockState(pos.down().down());
            if((blockStateDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty()) || (blockStateDownDown.isOf(Blocks.POINTED_DRIPSTONE) && blockStateDown.getFluidState().isEmpty())) {
                world.setBlockState(pos, Blocks.MUD.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            else if (nearbyMoisture != currentMoisture) {
                world.setBlockState(pos, state.with(MOISTURE, nearbyMoisture), Block.NOTIFY_LISTENERS);

            } else if (currentMoisture == 0 && !blockStateDown.isSideSolidFullSquare(world, pos, Direction.DOWN) && random.nextInt(200) == 1) {
                world.setBlockState(pos, Blocks.DIRT.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private static int getMoistureAmountNearby(WorldView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos.down());
        return getMoistureAmountNearby(world, pos, state);
    }

    private static int getMoistureAmountNearby(WorldView world, BlockPos pos, BlockState blockStateDown) {
        if(blockStateDown.isIn(BlockTags.FIRE) || blockStateDown.isIn(BlockTags.CAMPFIRES) || blockStateDown.isOf(Blocks.LAVA)) return -MAX_MOISTURE;

        Optional<Integer> moisture;
        int moistureAmount = blockStateDown.isAir() ? -1 : 0;

        BlockState neighborState;
        BlockPos[] neighborsPos = new BlockPos[] {pos.up(), pos.north(), pos.south(), pos.west(), pos.east()};
        for (BlockPos blockPos : neighborsPos) {
            neighborState = world.getBlockState(blockPos);
            if (neighborState.isOf(Blocks.FIRE)) return -MAX_MOISTURE;
            if (neighborState.getFluidState().isIn(FluidTags.WATER)) return MAX_MOISTURE;

            moisture = neighborState.getOrEmpty(MOISTURE);
            if (moisture.isPresent() && moisture.get() > 0) moistureAmount += Math.min(2,  (moisture.get() + 1)/2);
        }

        return Math.min(3, Math.max(0, moistureAmount/2));
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && entity.getBlockStateAtPos().isOf(this) && !canWalkOnWetMud(entity)) {
            Vec3d velocity = entity.getVelocity();
            if(entity instanceof LivingEntity livingEntity) {

                // When a player goes inside mud, break a fragile block that was on top:
                double entityRelativeY = entity.getY() - Math.floor(entity.getY());
                if(entityRelativeY < 0.6) {
                    Block blockOnTop = world.getBlockState(pos.up()).getBlock();
                    if(blockOnTop.getHardness() < 0.2f || blockOnTop instanceof PlantBlock) world.breakBlock(pos.up(), true);
                }

                // When an entity has their head inside of mud, make them drown:
                boolean canEntityBeDamaged = shouldDamage(world, livingEntity);
                if(canEntityBeDamaged && world.getTime() % 30L == 0) {
                    entity.damage(CustomDamageSources.getMudSuffocation(world), 1);
                }

                // When an entity is jumping, they should be able to get out of the block, or at least stop falling:
                if(((LivingEntityAccessor)livingEntity).isJumping())  {
                    if(entity.isInFluid()) {
                        entity.slowMovement(state, new Vec3d(1.1, 1.0, 1.1));
                        entity.setVelocity(velocity.add(0.0, 0.03, 0.0));
                    }
                    else {
                        entity.slowMovement(state, new Vec3d(1.1, entityRelativeY < 0.9 ? 0.15 : 0.0, 1.1));
                    }
                }

                // Otherwise, when the entity moves, they'll get hurt:
                else if(hasEntityMoved(entity)) {
                    if(canEntityBeDamaged && world.getTime() % 5L == 0) entity.damage(CustomDamageSources.getMudSuffocation(world), 1);
                    entity.slowMovement(state, new Vec3d(0.98, 0.95, 0.98));
                }
                else {
                    entity.slowMovement(state, new Vec3d(0.95, 0.7, 0.95));
                }
            }
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (fallDistance < 1.5f || (!(entity instanceof LivingEntity) && !(entity instanceof FallingBlockEntity))) return;
        if(entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;

        BlockState blockState;
        if(state.get(MOISTURE) <= MAX_MOISTURE_SOLID) {
            if(fallDistance > 8.0f) blockState = Blocks.PACKED_MUD.getDefaultState();
            else if(entity instanceof FallingBlockEntity fallingBlock && fallingBlock.getBlockState().isIn(BlockTags.ANVIL)) blockState = Blocks.PACKED_MUD.getDefaultState();
            else blockState = Blocks.DIRT.getDefaultState();
        }
        else {
            if(fallDistance < 4.0f) return;
            else if(fallDistance < 10.0f) blockState = CustomBlocks.BROWN_MUD.getDefaultState();
            else blockState = FarmlandBlock.pushEntitiesUpBeforeBlockChange(state, Blocks.PACKED_MUD.getDefaultState(), world, pos);
        }
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
        super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5F);
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(state.get(MOISTURE) <= MAX_MOISTURE_SOLID) return VoxelShapes.fullCube();

        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext)context).getEntity()) != null && entity instanceof LivingEntity livingEntity) {
            return BrownMudBlock.canWalkOnWetMud(livingEntity) ? VoxelShapes.fullCube() : VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }


    public boolean shouldDamage(World world, LivingEntity entity) {
        if(entity instanceof PigEntity) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.04), entity.getBlockZ())).isOf(this);
    }

    public boolean hasEntityMoved(Entity entity) {
        return entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ() || entity.prevYaw != entity.getYaw() || entity.prevPitch != entity.getPitch();
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof WaterCreatureEntity || entity instanceof PigEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return (state.get(MOISTURE) > MAX_MOISTURE_SOLID);
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 0.2f;
    }


    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if(placer.isSneaking()) return;
        BlockState blockStateUp = world.getBlockState(pos.up());
        if(blockStateUp.isOf(Blocks.FIRE) || world.getDimension().ultrawarm()) {
            world.setBlockState(pos, Blocks.PACKED_MUD.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }

        int moisture = BrownMudBlock.getMoistureAmountNearby(world, pos);
        if(moisture > MAX_MOISTURE_SOLID) {
            world.setBlockState(pos, state.with(MOISTURE, moisture), Block.NOTIFY_LISTENERS);
            BrownMudBlock.mudifyNeighborBlock(world, pos.north(), moisture - 3);
            BrownMudBlock.mudifyNeighborBlock(world, pos.south(), moisture - 3);
            BrownMudBlock.mudifyNeighborBlock(world, pos.west(), moisture - 3);
            BrownMudBlock.mudifyNeighborBlock(world, pos.east(), moisture - 3);
            BrownMudBlock.mudifyNeighborBlock(world, pos.down(), moisture - 2);
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(direction != Direction.DOWN && neighborState.getFluidState().isIn(FluidTags.WATER) && state.get(Properties.MOISTURE) < 4) {
            return state.with(Properties.MOISTURE, 4);
        }
        else if(direction == Direction.UP && !state.canPlaceAt(world, pos)) {
            world.scheduleBlockTick(pos, this, 1);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    private static void mudifyNeighborBlock(World world, BlockPos pos, int moisture) {
        if(moisture < 0) return;
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if(block == Blocks.DIRT) world.setBlockState(pos, CustomBlocks.BROWN_MUD.getDefaultState().with(MOISTURE, moisture), Block.NOTIFY_LISTENERS);
    }
}
