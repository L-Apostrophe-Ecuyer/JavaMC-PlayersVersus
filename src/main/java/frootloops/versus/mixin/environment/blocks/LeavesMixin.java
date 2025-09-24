package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;

@Mixin(LeavesBlock.class)
public abstract class LeavesMixin extends Block implements Waterloggable {

    public LeavesMixin(Settings settings) {
        super(settings);
    }
    private static final double FALL_DISTANCE_REDUCTION = 3.0;
    private static final double MIN_VELOCITY_TO_BE_SOLID = -0.5;
    private static Vec3d NORMAL_MULT = new Vec3d(0.6, 0.8, 0.6);
    private static Vec3d SNEAKING_MULT = new Vec3d(0.8, 0.8, 0.8);

    private static final VoxelShape COLLISION_SHAPE_INSIDE = Block.createCuboidShape(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    protected VoxelShape getInsideCollisionShape(BlockState state, BlockView world, BlockPos pos, Entity entity) {
        if(entity instanceof LivingEntity && entity.getVelocity().y < MIN_VELOCITY_TO_BE_SOLID) return VoxelShapes.empty();
        return COLLISION_SHAPE_INSIDE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext && ((EntityShapeContext) context).getEntity() instanceof LivingEntity livingEntity) {
            if(livingEntity.hasPassengers() || livingEntity.hasVehicle() || livingEntity.getVelocity().y < MIN_VELOCITY_TO_BE_SOLID) {
                livingEntity.fallDistance = -(livingEntity.getVelocity().getY() + 0.07) * 8.0;
                livingEntity.setVelocity(livingEntity.getVelocity().multiply(1.0, 0.9, 1.0));
                return VoxelShapes.empty();
            }
            if(livingEntity instanceof SpiderEntity || livingEntity instanceof Flutterer || livingEntity instanceof AnimalEntity) return VoxelShapes.fullCube();
            if(!context.isAbove(VoxelShapes.fullCube(), pos, true)) return VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            if(fallingBlock.getBlockState().getSoundGroup() != BlockSoundGroup.ANVIL) return;
            Block.dropStacks(state, world, pos);
            world.removeBlock(pos, false);
        }
        else if(entity instanceof LivingEntity livingEntity) {
            fallDistance = Math.max(fallDistance, -(livingEntity.getVelocity().getY() + 0.07) * 8.0);
            if(livingEntity.getVelocity().y < MIN_VELOCITY_TO_BE_SOLID) {
                livingEntity.fallDistance = fallDistance;
                livingEntity.setVelocity(livingEntity.getVelocity().x, -0.15, livingEntity.getVelocity().z);
            }
            else if(!state.getFluidState().isEmpty() && EnchantRegistryHelper.hasEnchantment(livingEntity.getEquippedStack(EquipmentSlot.FEET), Enchantments.FROST_WALKER)) {
                world.breakBlock(pos, true);
                world.setBlockState(pos, Blocks.FROSTED_ICE.getDefaultState());
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, Blocks.FROSTED_ICE.getDefaultState()));
                super.onLandedUpon(world, state, pos, livingEntity, fallDistance);
            }
            else {
                fallDistance = fallDistance / FALL_DISTANCE_REDUCTION;
                super.onLandedUpon(world, state, pos, livingEntity, fallDistance);
            }
        }
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler) {
        if (entity.hasVehicle() || entity.isSpectator()) return;
        if (!entity.getBlockPos().equals(pos) && !entity.getBlockPos().up().equals(pos)) return;
        if (entity instanceof LivingEntity livingEntity) {
            if(((LivingEntityAccessor)livingEntity).isJumping()) {
                if(livingEntity.isOnGround() || (livingEntity.getVelocity().y < -0.07 && livingEntity.getVelocity().y > -0.08)) {
                    livingEntity.addVelocity(0.0, 0.33 - livingEntity.getVelocity().y, 0.0);
                }
            }
            else {
                if (livingEntity.isSneaking()) entity.slowMovement(state, SNEAKING_MULT);
                else entity.slowMovement(state, NORMAL_MULT);
            }
        }
    }

    @Redirect(method = "getStateForNeighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;getDistanceFromLog(Lnet/minecraft/block/BlockState;)I"))
    private int getDistanceFromLog_OnUpdate(BlockState state) {
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        if(hasMatchingLog && LOG_MAP.get(state.getBlock()) == this) return 0;
        else if(!hasMatchingLog && state.isIn(BlockTags.LOGS)) return 0;
        else if(state.getBlock() == this) return state.get(LeavesBlock.DISTANCE);
        return 7;
    }

    @Redirect(method = "getPlacementState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;updateDistanceFromLogs(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState updateDistanceFromLogs_OnPlace(BlockState state, WorldAccess world, BlockPos pos) {
        if(state.get(LeavesBlock.PERSISTENT)) return state;
        return state.with(LeavesBlock.DISTANCE, getUpdatedDistance(state, world, pos));
    }

    private int getUpdatedDistance(BlockState state, WorldAccess world, BlockPos pos) {
        int i = 7;
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Direction direction : Direction.values()) {
            mutable.set(pos, direction);
            BlockState neighborState = world.getBlockState(mutable);
            if(hasMatchingLog && LOG_MAP.get(neighborState.getBlock()) == this) i = 1;
            else if(!hasMatchingLog && neighborState.isIn(BlockTags.LOGS)) i = 1;
            else if(neighborState.isOf(state.getBlock())){
                int d = getDistanceFromLog_OnUpdate(world.getBlockState(mutable));
                i = Math.min(i, d + 1);
            }
            if (i == 1) break;
        }
        if(i > LEAVES_WITH_MAX_DISTANCE.getOrDefault(this, 7)) return 7;
        return i;
    }

    @Overwrite
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(!(Boolean)state.get(LeavesBlock.PERSISTENT)) {
            int currentDistance = state.get(LeavesBlock.DISTANCE);
            int newDistance = getUpdatedDistance(state, world, pos);
            if(currentDistance == 1 && newDistance > 1) newDistance = 7;
            if(currentDistance != newDistance) world.setBlockState(pos, state.with(LeavesBlock.DISTANCE, newDistance), Block.NOTIFY_ALL);
        }
    }

    private static final HashMap<Block, Integer> LEAVES_WITH_MAX_DISTANCE = new HashMap<>();
    static {
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.SPRUCE_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.BIRCH_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.DARK_OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.PALE_OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.ACACIA_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.CHERRY_LEAVES, 5);
    }

    private static final HashMap<Block, Boolean> LEAVES_WITH_MATCHING_LOG = new HashMap<>();
    static {
        LEAVES_WITH_MATCHING_LOG.put(Blocks.OAK_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.SPRUCE_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.BIRCH_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.DARK_OAK_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.JUNGLE_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.ACACIA_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.CHERRY_LEAVES, true);
    }

    private static final HashMap<Block, Block> LOG_MAP = new HashMap<>();
    static {
        LOG_MAP.put(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        LOG_MAP.put(Blocks.OAK_WOOD, Blocks.OAK_LEAVES);
        LOG_MAP.put(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
        LOG_MAP.put(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LEAVES);
        LOG_MAP.put(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        LOG_MAP.put(Blocks.BIRCH_WOOD, Blocks.BIRCH_LEAVES);
        LOG_MAP.put(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        LOG_MAP.put(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LEAVES);
        LOG_MAP.put(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
        LOG_MAP.put(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LEAVES);
        LOG_MAP.put(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);
        LOG_MAP.put(Blocks.ACACIA_WOOD, Blocks.ACACIA_LEAVES);
        LOG_MAP.put(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        LOG_MAP.put(Blocks.CHERRY_WOOD, Blocks.CHERRY_LEAVES);
    }
}
